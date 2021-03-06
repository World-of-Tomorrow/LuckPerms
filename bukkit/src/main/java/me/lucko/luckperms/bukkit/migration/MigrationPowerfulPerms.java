/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.bukkit.migration;

import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResultRunnable;
import com.google.common.util.concurrent.ListenableFuture;
import com.zaxxer.hikari.HikariDataSource;

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.bukkit.migration.utils.LPResultRunnable;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.ProgressLogger;

import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static me.lucko.luckperms.common.constants.Permission.MIGRATION;


public class MigrationPowerfulPerms extends SubCommand<Object> {
    private static Method getPlayerPermissionsMethod = null;
    private static Method getPlayerGroupsMethod = null;
    private static Method getGroupMethod = null;

    // lol, nice "api"
    private static boolean superLegacy = false;
    private static boolean legacy = false;

    static {
        try {
            Class.forName("com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable");
            legacy = true;
        } catch (ClassNotFoundException ignored) {}

        if (legacy) {
            try {
                getPlayerPermissionsMethod = PermissionManager.class.getMethod("getPlayerOwnPermissions", UUID.class, ResultRunnable.class);
                getPlayerPermissionsMethod.setAccessible(true);
            } catch (NoSuchMethodException ignored) {}
        } else {
            try {
                getPlayerPermissionsMethod = PermissionManager.class.getMethod("getPlayerOwnPermissions", UUID.class);
                getPlayerPermissionsMethod.setAccessible(true);
            } catch (NoSuchMethodException ignored) {}
        }

        try {
            getGroupMethod = CachedGroup.class.getMethod("getGroup");
            getGroupMethod.setAccessible(true);
            superLegacy = true;
        } catch (NoSuchMethodException ignored) {}

        if (!legacy) {
            try {
                getPlayerGroupsMethod = PermissionManager.class.getMethod("getPlayerOwnGroups", UUID.class);
                getPlayerGroupsMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else {
            try {
                getPlayerGroupsMethod = PermissionManager.class.getMethod("getPlayerOwnGroups", UUID.class, ResultRunnable.class);
                getPlayerGroupsMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                try {
                    getPlayerGroupsMethod = PermissionManager.class.getMethod("getPlayerGroups", UUID.class, ResultRunnable.class);
                    getPlayerGroupsMethod.setAccessible(true);
                } catch (NoSuchMethodException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public MigrationPowerfulPerms() {
        super("powerfulperms", "Migration from PowerfulPerms", MIGRATION, Predicates.not(5),
                Arg.list(
                        Arg.create("address", true, "the address of the PP database"),
                        Arg.create("database", true, "the name of the PP database"),
                        Arg.create("username", true, "the username to log into the DB"),
                        Arg.create("password", true, "the password to log into the DB"),
                        Arg.create("db table", true, "the name of the PP table where player data is stored")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        try {
            return run(plugin, sender, args);
        } catch (Throwable t) {
            t.printStackTrace();
            return CommandResult.FAILURE;
        }
    }

    private CommandResult run(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        ProgressLogger log = new ProgressLogger("PowerfulPerms");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");
        
        if (!Bukkit.getPluginManager().isPluginEnabled("PowerfulPerms")) {
            log.logErr("PowerfulPerms is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        final String address = args.get(0);
        final String database = args.get(1);
        final String username = args.get(2);
        final String password = args.get(3);
        final String dbTable = args.get(4);

        // Find a list of UUIDs
        log.log("Getting a list of UUIDs to migrate.");
        Set<UUID> uuids = new HashSet<>();

        try (HikariDataSource hikari = new HikariDataSource()) {
            hikari.setMaximumPoolSize(2);
            hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
            hikari.addDataSourceProperty("serverName", address.split(":")[0]);
            hikari.addDataSourceProperty("port", address.split(":")[1]);
            hikari.addDataSourceProperty("databaseName", database);
            hikari.addDataSourceProperty("user", username);
            hikari.addDataSourceProperty("password", password);

            try (Connection c = hikari.getConnection()) {
                DatabaseMetaData meta = c.getMetaData();

                try (ResultSet rs = meta.getTables(null, null, dbTable, null)) {
                    if (!rs.next()) {
                        log.log("Error - Couldn't find table.");
                        return CommandResult.FAILURE;
                    }
                }
            }

            try (Connection c = hikari.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement("SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=?")) {
                    ps.setString(1, dbTable);

                    try (ResultSet rs = ps.executeQuery()) {
                        log.log("Found table: " + dbTable);
                        while (rs.next()) {
                            log.log("" + rs.getString("COLUMN_NAME") + " - " + rs.getString("COLUMN_TYPE"));
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement("SELECT `uuid` FROM " + dbTable)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            uuids.add(UUID.fromString(rs.getString("uuid")));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (uuids.isEmpty()) {
            log.logErr("Unable to find any UUIDs to migrate.");
            return CommandResult.FAILURE;
        }

        log.log("Found " + uuids.size() + " uuids. Starting migration.");

        PowerfulPermsPlugin ppPlugin = (PowerfulPermsPlugin) Bukkit.getPluginManager().getPlugin("PowerfulPerms");
        PermissionManager pm = ppPlugin.getPermissionManager();

        // Groups first.
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        Map<Integer, Group> groups = pm.getGroups(); // All versions
        for (Group g : groups.values()) {
            plugin.getStorage().createAndLoadGroup(g.getName().toLowerCase(), CreationCause.INTERNAL).join();
            final me.lucko.luckperms.common.core.model.Group group = plugin.getGroupManager().getIfLoaded(g.getName().toLowerCase());

            for (Permission p : g.getOwnPermissions()) { // All versions
                applyPerm(group, p, log);
            }

            for (Group parent : g.getParents()) { // All versions
                try {
                    group.setPermission("group." + parent.getName().toLowerCase(), true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        }
        log.log("Migrated " + groupCount.get() + " groups");

        // Now users.
        log.log("Starting user migration.");
        final Map<UUID, CountDownLatch> progress = new HashMap<>();

        // Migrate all users and their groups
        for (UUID uuid : uuids) {
            progress.put(uuid, new CountDownLatch(2));

            // Create a LuckPerms user for the UUID
            plugin.getStorage().loadUser(uuid, "null").join();
            User user = plugin.getUserManager().get(uuid);

            // Get a list of Permissions held by the user from the PP API.
            getPlayerPermissions(pm, uuid, perms -> { // Changes each version
                perms.forEach(p -> applyPerm(user, p, log));

                // Update the progress so the user can be saved and unloaded.
                synchronized (progress) {
                    progress.get(uuid).countDown();
                    if (progress.get(uuid).getCount() == 0) {
                        plugin.getStorage().saveUser(user);
                        plugin.getUserManager().cleanup(user);
                    }
                }
            });

            // Migrate the user's groups to LuckPerms from PP.
            Consumer<Map<String, List<CachedGroup>>> callback = groups1 -> {
                for (Map.Entry<String, List<CachedGroup>> e : groups1.entrySet()) {
                    final String server;
                    if (e.getKey() != null && (e.getKey().equals("") || e.getKey().equalsIgnoreCase("all"))) {
                        server = null;
                    } else {
                        server = e.getKey();
                    }

                    if (superLegacy) {
                        e.getValue().stream()
                                .filter(cg -> !cg.isNegated())
                                .map(cg -> {
                                    try {
                                        return (Group) getGroupMethod.invoke(cg);
                                    } catch (IllegalAccessException | InvocationTargetException e1) {
                                        e1.printStackTrace();
                                        return null;
                                    }
                                })
                                .forEach(g -> {
                                    if (g != null) {
                                        if (server == null) {
                                            try {
                                                user.setPermission("group." + g.getName().toLowerCase(), true);
                                            } catch (Exception ex) {
                                                log.handleException(ex);
                                            }
                                        } else {
                                            try {
                                                user.setPermission("group." + g.getName().toLowerCase(), true, server);
                                            } catch (Exception ex) {
                                                log.handleException(ex);
                                            }
                                        }
                                    }
                                });
                    } else {
                        e.getValue().stream()
                                .filter(g -> !g.hasExpired() && !g.isNegated())
                                .forEach(g -> {
                                    final Group group = pm.getGroup(g.getGroupId());
                                    if (g.willExpire()) {
                                        if (server == null) {
                                            try {
                                                user.setPermission("group." + group.getName().toLowerCase(), true, g.getExpirationDate().getTime() / 1000L);
                                            } catch (Exception ex) {
                                                log.handleException(ex);
                                            }
                                        } else {
                                            try {
                                                user.setPermission("group." + group.getName().toLowerCase(), true, server, g.getExpirationDate().getTime() / 1000L);
                                            } catch (Exception ex) {
                                                log.handleException(ex);
                                            }
                                        }

                                    } else {
                                        if (server == null) {
                                            try {
                                                user.setPermission("group." + group.getName().toLowerCase(), true);
                                            } catch (Exception ex) {
                                                log.handleException(ex);
                                            }
                                        } else {
                                            try {
                                                user.setPermission("group." + group.getName().toLowerCase(), true, server);
                                            } catch (Exception ex) {
                                                log.handleException(ex);
                                            }
                                        }
                                    }
                                });
                    }
                }

                // Update the progress so the user can be saved and unloaded.
                synchronized (progress) {
                    progress.get(uuid).countDown();
                    if (progress.get(uuid).getCount() == 0) {
                        plugin.getStorage().saveUser(user);
                        plugin.getUserManager().cleanup(user);
                    }
                }
            };

            if (!legacy) {
                try {
                    ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> future = (ListenableFuture<LinkedHashMap<String, List<CachedGroup>>>) getPlayerGroupsMethod.invoke(pm, uuid);
                    try {
                        if (future.isDone()) {
                            callback.accept(future.get());
                        } else {
                            future.addListener(() -> {
                                try {
                                    callback.accept(future.get());
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }, Runnable::run);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    getPlayerGroupsMethod.invoke(pm, uuid, new LPResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {
                        @Override
                        public void run() {
                            callback.accept(getResult());
                        }
                    });
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        // All groups are migrated, but there may still be some users being migrated.
        // This block will wait for all users to be completed.
        log.log("Waiting for user migration to complete. This may take some time");
        boolean sleep = true;
        while (sleep) {
            sleep = false;

            for (Map.Entry<UUID, CountDownLatch> e : progress.entrySet()) {
                if (e.getValue().getCount() != 0) {
                    sleep = true;
                    break;
                }
            }

            if (sleep) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        // We done.
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }

    private void applyPerm(PermissionHolder holder, Permission p, ProgressLogger log) {
        String node = p.getPermissionString();
        boolean value = true;
        if (node.startsWith("!")) {
            node = node.substring(1);
            value = false;
        }

        String server = p.getServer();
        if (server != null && server.equalsIgnoreCase("all")) {
            server = null;
        }

        String world = p.getWorld();
        if (world != null && world.equalsIgnoreCase("all")) {
            world = null;
        }

        long expireAt = 0L;
        if (!superLegacy) {
            if (p.willExpire()) {
                expireAt = p.getExpirationDate().getTime() / 1000L;
            }
        }

        if (world != null && server == null) {
            server = "global";
        }

        if (world != null) {
            if (expireAt == 0L) {
                try {
                    holder.setPermission(node, value, server, world);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            } else {
                try {
                    holder.setPermission(node, value, server, world, expireAt);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

        } else if (server != null) {
            if (expireAt == 0L) {
                try {
                    holder.setPermission(node, value, server);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            } else {
                try {
                    holder.setPermission(node, value, server, expireAt);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }
        } else {
            if (expireAt == 0L) {
                try {
                    holder.setPermission(node, value);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            } else {
                try {
                    holder.setPermission(node, value, expireAt);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }
        }
    }

    private static void getPlayerPermissions(PermissionManager manager, UUID uuid, Consumer<List<Permission>> callback) {
        if (legacy) {
            try {
                getPlayerPermissionsMethod.invoke(manager, uuid, new LPResultRunnable<List<Permission>>() {
                    @Override
                    public void run() {
                        callback.accept(getResult());
                    }
                });
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ListenableFuture<List<Permission>> lf = (ListenableFuture<List<Permission>>) getPlayerPermissionsMethod.invoke(manager, uuid);
                try {
                    if (lf.isDone()) {
                        callback.accept(lf.get());
                    } else {
                        lf.addListener(() -> {
                            try {
                                callback.accept(lf.get());
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }, Runnable::run);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
