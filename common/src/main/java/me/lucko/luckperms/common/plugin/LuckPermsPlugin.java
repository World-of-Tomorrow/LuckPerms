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

package me.lucko.luckperms.common.plugin;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.common.api.ApiProvider;
import me.lucko.luckperms.common.caching.handlers.CachedStateManager;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.commands.BaseCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Importer;
import me.lucko.luckperms.common.debug.DebugHandler;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.UserManager;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.treeview.PermissionVault;
import me.lucko.luckperms.common.utils.BufferedRequest;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Main internal interface for LuckPerms plugins, providing the base for abstraction throughout the project.
 *
 * All plugin platforms implement this interface.
 */
public interface LuckPermsPlugin {

    /**
     * Gets the user manager instance for the platform
     *
     * @return the user manager
     */
    UserManager getUserManager();

    /**
     * Gets the group manager instance for the platform
     *
     * @return the group manager
     */
    GroupManager getGroupManager();

    /**
     * Gets the track manager instance for the platform
     *
     * @return the track manager
     */
    TrackManager getTrackManager();

    /**
     * Gets the plugin's configuration
     *
     * @return the plugin config
     */
    LuckPermsConfiguration getConfiguration();

    /**
     * Gets the primary data storage instance. This is likely to be wrapped with extra layers for caching, etc.
     *
     * @return the storage handler instance
     */
    Storage getStorage();

    /**
     * Gets the redis messaging instance if present. Could return null if redis is not enabled.
     *
     * @return the redis messaging service
     */
    InternalMessagingService getMessagingService();

    /**
     * Gets a wrapped logger instance for the platform.
     *
     * @return the plugin's logger
     */
    Logger getLog();

    /**
     * Gets the UUID caching store for the platform
     *
     * @return the uuid cache
     */
    UuidCache getUuidCache();

    /**
     * Returns the class implementing the LuckPermsAPI on this platform.
     *
     * @return the api
     */
    ApiProvider getApiProvider();

    /**
     * Gets the importer instance
     *
     * @return the importer
     */
    Importer getImporter();

    /**
     * Gets the instance providing locale translations for the plugin
     *
     * @return the locale manager
     */
    LocaleManager getLocaleManager();

    /**
     * Gets the context manager.
     * This object handles context accumulation for all players on the platform.
     *
     * @return the context manager
     */
    ContextManager getContextManager();

    /**
     * Gets the cached state manager for the platform.
     *
     * @return the cached state manager
     */
    CachedStateManager getCachedStateManager();

    /**
     * Gets the class responsible for constructing PermissionCalculators on this platform.
     *
     * @return the permission calculator factory
     */
    CalculatorFactory getCalculatorFactory();

    /**
     * Gets the verbose debug handler instance.
     *
     * @return the debug handler instance
     */
    DebugHandler getDebugHandler();

    /**
     * Gets the permission caching instance for the platform.
     *
     * @return the permission cache instance
     */
    PermissionVault getPermissionVault();

    /**
     * Gets the LuckPerms Scheduler instance
     *
     * @return the scheduler
     */
    LuckPermsScheduler getScheduler();

    default void doAsync(Runnable r) {
        getScheduler().doAsync(r);
    }

    default void doSync(Runnable r) {
        getScheduler().doSync(r);
    }

    /**
     * Gets a string of the plugin's version
     *
     * @return the version of the plugin
     */
    String getVersion();

    /**
     * Gets the platform type this instance of LuckPerms is running on.
     *
     * @return the platform type
     */
    PlatformType getServerType();

    /**
     * Gets the name or "brand" of the running platform
     * @return the server brand
     */
    String getServerName();

    /**
     * Gets the version of the running platform
     * @return the server version
     */
    String getServerVersion();

    /**
     * Gets the plugins main data storage directory
     *
     * <p>Bukkit: /root/plugins/LuckPerms</p>
     * <p>Bungee: /root/plugins/LuckPerms</p>
     * <p>Sponge: /root/luckperms/</p>
     *
     * @return the platforms data folder
     */
    File getDataDirectory();

    /**
     * Gets the plugins config directory.
     *
     * <p>This is the same as {@link #getDataDirectory()} on Bukkit/Bungee, but different on Sponge.</p>
     *
     * @return the platforms config folder
     */
    default File getConfigDirectory() {
        return getDataDirectory();
    }

    /**
     * Gets a bundled resource file from the jar
     *
     * @param path the path of the file
     * @return the file as an input stream
     */
    InputStream getResourceStream(String path);

    /**
     * Returns a colored string indicating the status of a player
     *
     * @param uuid The player's uuid
     * @return a formatted status string
     */
    default Message getPlayerStatus(UUID uuid) {
        UUID external = getUuidCache().getExternalUUID(uuid);
        return isPlayerOnline(external) ? Message.PLAYER_ONLINE : Message.PLAYER_OFFLINE;
    }

    /**
     * Gets a player object linked to this User. The returned object must be the same type
     * as the instance used in the platforms {@link ContextManager}
     *
     * @param user the user instance
     * @return a player object, or null, if one couldn't be found.
     */
    Object getPlayer(User user);

    /**
     * Gets a calculated context instance for the user using the rules of the platform.
     *
     * @param user the user instance
     * @return a contexts object, or null if one couldn't be generated
     */
    Contexts getContextForUser(User user);

    /**
     * Gets the number of users online on the platform
     *
     * @return the number of users
     */
    int getPlayerCount();

    /**
     * Gets the usernames of the users online on the platform
     *
     * @return a {@link List} of usernames
     */
    List<String> getPlayerList();

    /**
     * Gets the UUIDs of the users online on the platform
     *
     * @return a {@link Set} of UUIDs
     */
    Set<UUID> getOnlinePlayers();

    /**
     * Checks if a user is online
     *
     * @param external the users external uuid
     * @return true if the user is online
     */
    boolean isPlayerOnline(UUID external);

    /**
     * Gets a list of online Senders on the platform
     *
     * @return a {@link List} of senders online on the platform
     */
    List<Sender> getOnlineSenders();

    /**
     * Gets the console.
     *
     * @return the console sender of the instance
     */
    Sender getConsoleSender();

    /**
     * Gets a set of Contexts that should be pre-processed in advance
     *
     * @param op if the user being processed is op
     * @return a set of contexts
     */
    Set<Contexts> getPreProcessContexts(boolean op);

    default List<BaseCommand> getExtraCommands() {
        return Collections.emptyList();
    }

    /**
     * Gets a map of extra information to be shown in the info command
     *
     * @return a map of options, or null
     */
    default LinkedHashMap<String, Object> getExtraInfo() {
        return null;
    }

    /**
     * Gets a set of players ignoring logging output
     *
     * @return a {@link Set} of {@link UUID}s
     */
    Set<UUID> getIgnoringLogs();

    /**
     * Gets the UUID of a player. Used as a backup for migration
     *
     * @param playerName the players name
     * @return a uuid if found, or null if not
     */
    UUID getUuidFromUsername(String playerName);

    /**
     * Gets the update task buffer of the platform, used for scheduling and running update tasks.
     *
     * @return the update task buffer instance
     */
    BufferedRequest<Void> getUpdateTaskBuffer();

    /**
     * Called at the end of the sync task.
     */
    default void onPostUpdate() {

    }

    /**
     * Called when a users data is refreshed
     *
     * @param user the user
     */
    default void onUserRefresh(User user) {

    }

    static void sendStartupBanner(Sender sender, LuckPermsPlugin plugin) {
        sender.sendMessage(Util.color("&b               __       &3 __   ___  __         __  "));
        sender.sendMessage(Util.color("&b    |    |  | /  ` |__/ &3|__) |__  |__)  |\\/| /__` "));
        sender.sendMessage(Util.color("&b    |___ \\__/ \\__, |  \\ &3|    |___ |  \\  |  | .__/ "));
        sender.sendMessage(Util.color(" "));
        sender.sendMessage(Util.color("&2  Loading version &bv" + plugin.getVersion() + "&2 on " + plugin.getServerType().getFriendlyName() + " - " + plugin.getServerName()));
        sender.sendMessage(Util.color(" "));
    }

}
