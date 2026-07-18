package pkg

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/robinbraemer/event"
	"go.minekube.com/brigodier"
	"go.minekube.com/common/minecraft/component"
	"go.minekube.com/gate/pkg/command"
	"go.minekube.com/gate/pkg/util/uuid"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"

	"github.com/go-logr/logr"
	"go.minekube.com/gate/pkg/edition/java/proxy"
)

type ProxyServerInfoGetter struct {
	proxy *proxy.Proxy
}

func NewProxyServerInfoGetter(p *proxy.Proxy) *ProxyServerInfoGetter {
	return &ProxyServerInfoGetter{proxy: p}
}

type ServerConnectionInfo struct {
	Name          string
	OnlinePlayers int
}

func (g *ProxyServerInfoGetter) Get() []ServerConnectionInfo {
	servers := g.proxy.Servers()
	result := make([]ServerConnectionInfo, 0, len(servers))

	for name, server := range servers {
		result = append(result, ServerConnectionInfo{
			Name:          strconv.Itoa(name),
			OnlinePlayers: server.Players().Len(),
		})
	}

	return result
}

type ServerConnectionPlugin struct {
	dataDirectory              string
	proxy                      *proxy.Proxy
	serverConnectionInfoGetter ServerConnectionInfoGetter
	config                     Config
	log                        logr.Logger
}

func NewServerConnectionPlugin(
	dataDirectory string,
	p *proxy.Proxy,
	serverConnectionInfoGetter ServerConnectionInfoGetter,
	log logr.Logger,
) *ServerConnectionPlugin {
	return &ServerConnectionPlugin{
		dataDirectory:              dataDirectory,
		proxy:                      p,
		serverConnectionInfoGetter: serverConnectionInfoGetter,
		log:                        log,
	}
}

func (s *ServerConnectionPlugin) Init() error {
	config, err := s.loadOrCreateConfig()
	if err != nil {
		return err
	}
	s.config = config

	for _, cmdConfig := range s.config.Commands {
		cmdData := s.registerCommand(cmdConfig)

		s.proxy.Command().Register(cmdData)
	}

	s.registerEventListeners()

	return nil
}

func (s *ServerConnectionPlugin) loadOrCreateConfig() (Config, error) {
	configPath := filepath.Join(s.dataDirectory, "connection-config.json")

	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		defaultConfig := createDefaultConfig()

		if err := os.MkdirAll(s.dataDirectory, 0755); err != nil {
			return Config{}, err
		}

		file, err := os.Create(configPath)
		if err != nil {
			return Config{}, err
		}
		defer file.Close()

		encoder := json.NewEncoder(file)
		encoder.SetIndent("", "  ")
		if err := encoder.Encode(defaultConfig); err != nil {
			return Config{}, err
		}

		return defaultConfig, nil
	}

	file, err := os.Open(configPath)
	if err != nil {
		return Config{}, err
	}
	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			s.log.Error(err, "Failed to close config file")

			return
		}
	}(file)

	var config Config
	if err := json.NewDecoder(file).Decode(&config); err != nil {
		return Config{}, err
	}

	return config, nil
}

func (s *ServerConnectionPlugin) registerCommand(cmdConfig CommandConfig) brigodier.LiteralNodeBuilder {
	regCommand := command.Command(func(ctx *command.Context) error {
		player, ok := ctx.Source.(proxy.Player)
		if !ok {
			return ctx.Source.SendMessage(&component.Text{Content: "You must be a player to run this command."})
		}
		var currentServer string
		if player.CurrentServer() != nil {
			currentServer = player.CurrentServer().Server().ServerInfo().Name()
		}

		serverName, found := s.getConnectionAndName(player.ID(), cmdConfig.TargetConnections, currentServer)
		if !found {
			player.SendMessage(&component.Text{Content: cmdConfig.NoTargetConnectionFound})
			return nil
		}

		if currentServer == serverName.serverName {
			player.SendMessage(&component.Text{Content: cmdConfig.AlreadyConnectedMessage})
			return nil
		}

		server := s.proxy.Server(serverName.serverName)
		if server == nil {
			player.SendMessage(&component.Text{Content: cmdConfig.NoTargetConnectionFound})
			s.log.Error(errors.New("failed to find command target server"),
				"serverName", serverName, "player", player.Username())
			return nil
		}

		s.log.Info("Player using command to connect to server",
			"command", cmdConfig.Name, "player", player.Username(), "server", serverName)
		player.CreateConnectionRequest(server).ConnectWithIndication(ctx.Context)

		return nil
	})

	return brigodier.Literal(cmdConfig.Name).Requires(command.Requires(func(c *command.RequiresContext) bool {
		if cmdConfig.Permission == "" {
			return true
		}
		return c.Source.HasPermission(cmdConfig.Permission)
	})).Executes(regCommand)
}

func (s *ServerConnectionPlugin) registerEventListeners() {
	event.Subscribe(s.proxy.Event(), 0, s.handleInitialConnect)

	event.Subscribe(s.proxy.Event(), 0, s.handleServerKick)
}

func (s *ServerConnectionPlugin) handleInitialConnect(event *proxy.PlayerChooseInitialServerEvent) {
	player := event.Player()

	serverName, found := s.GetConnectionAndNameForLogin(player.ID())
	if !found {
		msg := s.config.NetworkJoinTargets.NoTargetConnectionFoundMessage
		player.Disconnect(&component.Text{Content: msg})
		s.log.Info("Player disconnected due to no available target server",
			"player", player.Username())
		return
	}

	server := s.proxy.Server(serverName)
	if server == nil {
		msg := "No available server found"
		player.Disconnect(&component.Text{Content: msg})
		s.log.Error(errors.New("server not found"),
			"serverName", serverName, "player", player.Username())
		return
	}

	s.log.Info("Setting initial server for player",
		"player", player.Username(), "server", serverName)
	event.SetInitialServer(server)
}

func (s *ServerConnectionPlugin) handleServerKick(event *proxy.KickedFromServerEvent) {
	player := event.Player()
	serverFrom := event.Server().ServerInfo().Name()

	s.log.Info("Player was kicked from server",
		"player", player.Username(), "server", serverFrom,
		"reason", event.OriginalReason())

	if !s.config.FallbackConnectionsConfig.Enabled {
		return
	}

	serverName, found := s.GetConnectionAndNameForFallback(player.ID(), serverFrom)
	if !found {
		s.log.Info("No fallback server found for player",
			"player", player.Username())
		return
	}

	fallbackServer := s.proxy.Server(serverName)
	if fallbackServer == nil {
		s.log.Error(errors.New("failed to find fallback server"),
			"serverName", serverName, "player", player.Username())
		return
	}

	s.log.Info("Found fallback server for player",
		"player", player.Username(), "server", serverName)
	event.SetResult(&proxy.RedirectPlayerKickResult{
		Server: fallbackServer,
	})
}

func (s *ServerConnectionPlugin) GetConnectionAndNameForLogin(player uuid.UUID) (string, bool) {
	result, found := s.getConnectionAndName(player, s.config.NetworkJoinTargets.TargetConnections, "")
	if !found {
		return "", false
	}
	return result.serverName, true
}

func (s *ServerConnectionPlugin) GetConnectionAndNameForFallback(player uuid.UUID, fromServerName string) (string, bool) {
	result, found := s.getConnectionAndName(player, s.config.FallbackConnectionsConfig.TargetConnections, fromServerName)
	if !found {
		return "", false
	}
	return result.serverName, true
}

func (s *ServerConnectionPlugin) GetConnectionAndNameForCommand(player uuid.UUID, commandConfig CommandConfig) (string, bool) {
	result, found := s.getConnectionAndName(player, commandConfig.TargetConnections, "")
	if !found {
		return "", false
	}
	return result.serverName, true
}

type connectionResult struct {
	connectionAndTarget ConnectionAndTargetConfig
	serverName          string
}

func (s *ServerConnectionPlugin) getConnectionAndName(
	player uuid.UUID,
	targetConnections []TargetConnectionConfig,
	fromServerName string,
) (*connectionResult, bool) {
	possibleConnections := s.getPossibleServerConnections(player)

	var filteredTargetConnections []TargetConnectionConfig
	if fromServerName == "" {
		filteredTargetConnections = targetConnections
	} else {
		for _, tc := range targetConnections {
			if len(tc.From) == 0 || s.matchesFromPattern(tc, fromServerName) {
				filteredTargetConnections = append(filteredTargetConnections, tc)
			}
		}
	}

	var possibleConnectionsWithTarget []ConnectionAndTargetConfig
	for _, pc := range possibleConnections {
		for _, tc := range filteredTargetConnections {
			if pc.Name == tc.Name {
				possibleConnectionsWithTarget = append(possibleConnectionsWithTarget, ConnectionAndTargetConfig{
					ConnectionConfig: pc,
					TargetConfig:     tc,
				})
				break
			}
		}
	}

	if len(possibleConnectionsWithTarget) == 0 {
		return nil, false
	}

	sort.Slice(possibleConnectionsWithTarget, func(i, j int) bool {
		return possibleConnectionsWithTarget[i].TargetConfig.Priority > possibleConnectionsWithTarget[j].TargetConfig.Priority
	})

	bestConnectionAndTarget := possibleConnectionsWithTarget[0]

	bestServerToConnect := s.getBestServerToConnect(fromServerName, bestConnectionAndTarget.ConnectionConfig)
	if bestServerToConnect == "" {
		return nil, false
	}

	return &connectionResult{
		connectionAndTarget: bestConnectionAndTarget,
		serverName:          bestServerToConnect,
	}, true
}

func (s *ServerConnectionPlugin) matchesFromPattern(tc TargetConnectionConfig, serverName string) bool {
	for _, pattern := range tc.From {
		matched, err := regexp.MatchString(pattern, serverName)
		if err == nil && matched {
			return true
		}
	}
	return false
}

func (s *ServerConnectionPlugin) getPossibleServerConnections(player uuid.UUID) []ConnectionConfig {
	serverConnectionInfos := s.serverConnectionInfoGetter.Get()
	serverNames := make([]string, len(serverConnectionInfos))
	for i, info := range serverConnectionInfos {
		serverNames[i] = info.Name
	}

	var result []ConnectionConfig
	for _, connection := range s.config.Connections {
		if s.anyMatches(connection.ServerNameMatcher, serverNames) &&
			s.allRulesAllowed(connection.Rules, player) {
			result = append(result, connection)
		}
	}

	return result
}

func (s *ServerConnectionPlugin) anyMatches(matcher ServerMatcherConfiguration, serverNames []string) bool {
	for _, name := range serverNames {
		if s.matches(matcher, name) {
			return true
		}
	}
	return false
}

func (s *ServerConnectionPlugin) matches(matcher ServerMatcherConfiguration, serverName string) bool {
	switch matcher.Operation {
	case "STARTS_WITH":
		return strings.HasPrefix(serverName, matcher.Value)
	case "ENDS_WITH":
		return strings.HasSuffix(serverName, matcher.Value)
	case "CONTAINS":
		return strings.Contains(serverName, matcher.Value)
	case "EQUALS":
		return serverName == matcher.Value
	case "REGEX":
		matched, err := regexp.MatchString(matcher.Value, serverName)
		return err == nil && matched
	default:
		return false
	}
}

func (s *ServerConnectionPlugin) allRulesAllowed(rules []RulesConfig, player uuid.UUID) bool {
	for _, rule := range rules {
		if !s.isRuleAllowed(rule, player) {
			return false
		}
	}
	return true
}

func (s *ServerConnectionPlugin) isRuleAllowed(rule RulesConfig, player uuid.UUID) bool {
	if rule.Type == "PERMISSION" {
		hasPermission := s.proxy.Player(player).HasPermission(rule.Name)
		return (rule.Value == "true" && hasPermission) || (rule.Value == "false" && !hasPermission)
	}
	return false
}

func (s *ServerConnectionPlugin) getBestServerToConnect(fromServerName string, bestConnection ConnectionConfig) string {
	serverConnectionInfos := s.serverConnectionInfoGetter.Get()

	var matchingServers []ServerConnectionInfo
	for _, info := range serverConnectionInfos {
		if info.Name != fromServerName && s.matches(bestConnection.ServerNameMatcher, info.Name) {
			matchingServers = append(matchingServers, info)
		}
	}

	if len(matchingServers) == 0 {
		return ""
	}

	sort.Slice(matchingServers, func(i, j int) bool {
		return matchingServers[i].OnlinePlayers < matchingServers[j].OnlinePlayers
	})

	return matchingServers[0].Name
}

var Plugin = proxy.Plugin{
	Name: "simplecloud-connection-plugin",
	Init: func(ctx context.Context, p *proxy.Proxy) error {
		log := logr.FromContextOrDiscard(ctx)
		log.Info("Starting SimpleCloud Connection Plugin")

		dataDir := "simplecloud-connection"

		infoGetter := NewProxyServerInfoGetter(p)

		plugin := NewServerConnectionPlugin(dataDir, p, infoGetter, log)
		if err := plugin.Init(); err != nil {
			return err
		}

		log.Info("SimpleCloud Connection Plugin initialized successfully")
		return nil
	},
}
