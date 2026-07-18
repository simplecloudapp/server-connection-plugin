package pkg

type Config struct {
	Version                   string             `json:"version"`
	Connections               []ConnectionConfig `json:"connections"`
	NetworkJoinTargets        TargetsConfig      `json:"networkJoinTargets"`
	FallbackConnectionsConfig TargetsConfig      `json:"fallbackConnectionsConfig"`
	Commands                  []CommandConfig    `json:"commands"`
}

type ConnectionConfig struct {
	Name              string                     `json:"name"`
	ServerNameMatcher ServerMatcherConfiguration `json:"serverNameMatcher"`
	Rules             []RulesConfig              `json:"rules"`
}

type ServerMatcherConfiguration struct {
	Operation string `json:"operation"`
	Value     string `json:"value"`
}

type RulesConfig struct {
	Type  string `json:"type"`
	Name  string `json:"name"`
	Value string `json:"value"`
}

type TargetsConfig struct {
	Enabled                        bool                     `json:"enabled"`
	NoTargetConnectionFoundMessage string                   `json:"noTargetConnectionFoundMessage"`
	TargetConnections              []TargetConnectionConfig `json:"targetConnections"`
}

type TargetConnectionConfig struct {
	Name     string   `json:"name"`
	Priority int      `json:"priority"`
	From     []string `json:"from,omitempty"`
}

type CommandConfig struct {
	Name                    string                   `json:"name"`
	AlreadyConnectedMessage string                   `json:"alreadyConnectedMessage"`
	NoTargetConnectionFound string                   `json:"noTargetConnectionFound"`
	TargetConnections       []TargetConnectionConfig `json:"targetConnections"`
	Permission              string                   `json:"permission"`
	Aliases                 []string                 `json:"aliases"`
}

type ConnectionAndTargetConfig struct {
	ConnectionConfig ConnectionConfig
	TargetConfig     TargetConnectionConfig
}

type ServerConnectionInfoGetter interface {
	Get() []ServerConnectionInfo
}

func createDefaultConfig() Config {
	defaultConnections := []ConnectionConfig{
		{
			Name: "lobby",
			ServerNameMatcher: ServerMatcherConfiguration{
				Operation: "STARTS_WITH",
				Value:     "lobby",
			},
			Rules: []RulesConfig{},
		},
		{
			Name: "hub",
			ServerNameMatcher: ServerMatcherConfiguration{
				Operation: "STARTS_WITH",
				Value:     "hub",
			},
			Rules: []RulesConfig{},
		},
		{
			Name: "premium-lobby",
			ServerNameMatcher: ServerMatcherConfiguration{
				Operation: "STARTS_WITH",
				Value:     "premium",
			},
			Rules: []RulesConfig{
				{
					Type:  "PERMISSION",
					Name:  "simplecloud.connection.premium",
					Value: "true",
				},
			},
		},
		{
			Name: "vip-lobby",
			ServerNameMatcher: ServerMatcherConfiguration{
				Operation: "STARTS_WITH",
				Value:     "vip",
			},
			Rules: []RulesConfig{
				{
					Type:  "PERMISSION",
					Name:  "simplecloud.connection.vip",
					Value: "true",
				},
			},
		},
		{
			Name: "silent-lobby",
			ServerNameMatcher: ServerMatcherConfiguration{
				Operation: "STARTS_WITH",
				Value:     "silent",
			},
			Rules: []RulesConfig{
				{
					Type:  "PERMISSION",
					Name:  "simplecloud.connection.silent",
					Value: "true",
				},
			},
		},
	}

	defaultTargetConnections := []TargetConnectionConfig{
		{Name: "lobby", Priority: 0},
		{Name: "hub", Priority: 0},
		{Name: "premium-lobby", Priority: 10},
		{Name: "vip-lobby", Priority: 20},
		{Name: "silent-lobby", Priority: 20},
	}

	networkJoinTargets := TargetsConfig{
		Enabled:                        true,
		NoTargetConnectionFoundMessage: "<color:#dc2626>Couldn't connect you to the network because\nno target servers are available.",
		TargetConnections:              defaultTargetConnections,
	}

	fallbackConnectionsConfig := TargetsConfig{
		Enabled:                        true,
		NoTargetConnectionFoundMessage: "<color:#dc2626>You have been disconnected from the network\nbecause there are no fallback servers available.",
		TargetConnections:              defaultTargetConnections,
	}

	defaultCommands := []CommandConfig{
		{
			Name:                    "lobby",
			AlreadyConnectedMessage: "<color:#dc2626>You are already connected to this lobby!",
			NoTargetConnectionFound: "<color:#dc2626>Couldn't find a target server!",
			TargetConnections:       defaultTargetConnections,
			Aliases:                 []string{"l", "hub", "quit", "leave"},
		},
	}

	return Config{
		Version:                   "1",
		Connections:               defaultConnections,
		NetworkJoinTargets:        networkJoinTargets,
		FallbackConnectionsConfig: fallbackConnectionsConfig,
		Commands:                  defaultCommands,
	}
}
