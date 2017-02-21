/****** Object:  Table [dbo].[{prefix}actions]    Script Date: 2/20/2017 11:08:28 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[{prefix}actions](
	[id] [int] NOT NULL,
	[time] [bigint] NOT NULL,
	[actor_uuid] [varchar](36) NOT NULL,
	[actor_name] [varchar](16) NOT NULL,
	[type] [char](1) NOT NULL,
	[acted_uuid] [varchar](36) NOT NULL,
	[acted_name] [varchar](36) NOT NULL,
	[action] [varchar](256) NOT NULL,
 CONSTRAINT [PK_actions] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[{prefix}group_permissions]    Script Date: 2/20/2017 11:08:28 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[{prefix}group_permissions](
	[id] [int] NOT NULL,
	[name] [varchar](36) NOT NULL,
	[permission] [varchar](200) NOT NULL,
	[value] [bit] NOT NULL,
	[server] [varchar](36) NOT NULL,
	[world] [varchar](36) NOT NULL,
	[expiry] [int] NOT NULL,
	[contexts] [varchar](200) NOT NULL,
 CONSTRAINT [PK_group_permissions] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[{prefix}groups]    Script Date: 2/20/2017 11:08:28 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[{prefix}groups](
	[name] [varchar](36) NOT NULL,
 CONSTRAINT [PK_groups] PRIMARY KEY CLUSTERED 
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[{prefix}players]    Script Date: 2/20/2017 11:08:28 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[{prefix}players](
	[uuid] [varchar](36) NOT NULL,
	[username] [varchar](16) NOT NULL,
	[primary_group] [varchar](36) NOT NULL,
 CONSTRAINT [PK_players] PRIMARY KEY CLUSTERED 
(
	[uuid] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/****** Object:  Table [dbo].[{prefix}tracks]    Script Date: 2/20/2017 11:08:28 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[{prefix}tracks](
	[name] [varchar](36) NOT NULL,
	[groups] [text] NOT NULL,
 CONSTRAINT [PK_tracks] PRIMARY KEY CLUSTERED 
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
/****** Object:  Table [dbo].[{prefix}user_permissions]    Script Date: 2/20/2017 11:08:28 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[{prefix}user_permissions](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[uuid] [varchar](36) NOT NULL,
	[permission] [varchar](200) NOT NULL,
	[value] [bit] NOT NULL,
	[server] [varchar](36) NOT NULL,
	[world] [varchar](36) NOT NULL,
	[expiry] [int] NOT NULL,
	[contexts] [varchar](200) NOT NULL
) ON [PRIMARY]

GO
SET ANSI_PADDING ON

GO
/****** Object:  Index [{prefix}group_permissions_name]    Script Date: 2/20/2017 11:08:28 PM ******/
CREATE NONCLUSTERED INDEX [{prefix}group_permissions_name] ON [dbo].[{prefix}group_permissions]
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON

GO
/****** Object:  Index [{prefix}group_permissions_permission]    Script Date: 2/20/2017 11:08:28 PM ******/
CREATE NONCLUSTERED INDEX [{prefix}group_permissions_permission] ON [dbo].[{prefix}group_permissions]
(
	[permission] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON

GO
/****** Object:  Index [{prefix}players_username]    Script Date: 2/20/2017 11:08:28 PM ******/
CREATE NONCLUSTERED INDEX [{prefix}players_username] ON [dbo].[{prefix}players]
(
	[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON

GO
/****** Object:  Index [{prefix}user_permission_uuid]    Script Date: 2/20/2017 11:08:28 PM ******/
CREATE NONCLUSTERED INDEX [{prefix}user_permission_uuid] ON [dbo].[{prefix}user_permissions]
(
	[uuid] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON

GO
/****** Object:  Index [{prefix}user_permissions_permission]    Script Date: 2/20/2017 11:08:28 PM ******/
CREATE NONCLUSTERED INDEX [{prefix}user_permissions_permission] ON [dbo].[{prefix}user_permissions]
(
	[permission] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
EXEC sys.sp_addextendedproperty @name=N'MS_Description', @value=N'' , @level0type=N'SCHEMA',@level0name=N'dbo', @level1type=N'TABLE',@level1name=N'{prefix}user_permissions', @level2type=N'INDEX',@level2name=N'{prefix}user_permission_uuid'
GO