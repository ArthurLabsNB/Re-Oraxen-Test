package io.th0rgal.oraxen.pack.upload;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.pack.dispatch.PackDispatcher;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.generation.ResourcePack;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.pack.upload.hosts.Polymath;
import io.th0rgal.oraxen.pack.upload.hosts.Sh;
import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.utils.logs.Logs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.ProviderNotFoundException;
import java.util.List;

import static org.bukkit.Bukkit.getServer;

@SuppressWarnings("UnstableApiUsage")
public class UploadManager {

    private final Plugin plugin;
    private final boolean enabled;
    private final HostingProvider hostingProvider;

    private PackReceiver receiver;
    private PackSender sender;

    public UploadManager(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = (boolean) Pack.UPLOAD.getValue();
        this.hostingProvider = getHostingProvider();
    }

    public void uploadAsyncAndSendToPlayers(ResourcePack resourcePack) {
        if (!enabled)
            return;
        if ((boolean) Pack.RECEIVE_ENABLED.getValue() && receiver == null)
            Bukkit.getPluginManager().registerEvents(receiver = new PackReceiver(), plugin);
        long time = System.currentTimeMillis();
        Logs.log(ChatColor.GREEN, "Automatic upload of the resource pack is enabled, uploading...");
        Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), () -> {
            if (!hostingProvider.uploadPack(resourcePack.getFile())) {
                Logs.log(ChatColor.RED, "Resourcepack not uploaded");
                return;
            }
            if ((boolean) Pack.UPLOAD_BUNGEECORD.getValue() && OraxenPlugin.hasBungeeEnabled()) {
                try {
                    //ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("PackURL");
                    out.writeUTF(hostingProvider.getPackURL());
                    out.writeUTF("SHA1");
                    out.write(hostingProvider.getSHA1());
                    getServer().sendPluginMessage(OraxenPlugin.get(), "BungeeCord", out.toByteArray());
                } catch (Exception exception) {
                    Logs.log(ChatColor.DARK_RED, "Request to Bungeecord has failed : " + exception.getMessage() + "\nRessource pack on your server");
                    Logs.log(ChatColor.GREEN, "Resourcepack uploaded on url " + hostingProvider.getPackURL() + " in " + (System.currentTimeMillis() - time) + "ms");
                    PackDispatcher.setPackURL(hostingProvider.getPackURL());
                    PackDispatcher.setSha1(hostingProvider.getSHA1());
                    if (((boolean) Pack.SEND_PACK.getValue() || (boolean) Pack.SEND_JOIN_MESSAGE.getValue()) && sender == null)
                        Bukkit.getPluginManager().registerEvents(sender = new PackSender(), plugin);
                }
            } else {
                Logs
                        .log(ChatColor.GREEN, "Resourcepack uploaded on url " + hostingProvider.getPackURL() + " in "
                                + (System.currentTimeMillis() - time) + "ms");
                PackDispatcher.setPackURL(hostingProvider.getPackURL());
                PackDispatcher.setSha1(hostingProvider.getSHA1());
                if (((boolean) Pack.SEND_PACK.getValue() || (boolean) Pack.SEND_JOIN_MESSAGE.getValue()) && sender == null)
                    Bukkit.getPluginManager().registerEvents(sender = new PackSender(), plugin);
            }

        });
    }

    private HostingProvider getHostingProvider() {
        switch (Pack.UPLOAD_TYPE.toString().toLowerCase()) {
        case "polymath":
            return new Polymath(Pack.POLYMATH_SERVER.toString());
        case "sh":
        case "cmd":
            final ConfigurationSection opt = (ConfigurationSection) Pack.UPLOAD_OPTIONS.getValue();
            final List<String> args = opt.getStringList("args");
            if (args.isEmpty())
                throw new ProviderNotFoundException("No command line.");
            String placeholder = opt.getString("placeholder", "${file}");
            return new Sh(Sh.path(placeholder, args));
        case "external":
            Class<?> target;
            final ConfigurationSection options = (ConfigurationSection) Pack.UPLOAD_OPTIONS.getValue();
            String klass = options.getString("class");
            if (klass == null)
                throw new ProviderNotFoundException("No provider set.");
            try {
                target = Class.forName(klass);
            } catch (Throwable any) {
                ProviderNotFoundException error = new ProviderNotFoundException("Provider not found: " + klass);
                error.addSuppressed(any);
                throw error;
            }
            if (!HostingProvider.class.isAssignableFrom(target)) {
                throw new ProviderNotFoundException(target + " is not a valid HostingProvider.");
            }
            Class<? extends HostingProvider> implement = target.asSubclass(HostingProvider.class);
            Constructor<? extends HostingProvider> constructor;
            try {
                try {
                    constructor = implement.getConstructor(ConfigurationSection.class);
                } catch (Exception notFound) {
                    try {
                        constructor = implement.getConstructor();
                    } catch (Exception ignore) {
                        throw notFound; // Use (Lorg/bukkit/configuration/ConfigurationSection;)V to Exception
                    }
                }
            } catch (Exception e) {
                throw (ProviderNotFoundException) new ProviderNotFoundException("Cannot found constructor in " + target)
                    .initCause(e);
            }
            try {
                return constructor.getParameterCount() == 0 ? constructor.newInstance()
                    : constructor.newInstance(options);
            } catch (InstantiationException e) {
                throw (ProviderNotFoundException) new ProviderNotFoundException("Cannot alloc instance for " + target)
                    .initCause(e);
            } catch (IllegalAccessException e) {
                throw (ProviderNotFoundException) new ProviderNotFoundException("Failed to access " + target)
                    .initCause(e);
            } catch (InvocationTargetException e) {
                throw (ProviderNotFoundException) new ProviderNotFoundException("Exception in allocating instance.")
                    .initCause(e.getCause());
            }
        default:
            throw new ProviderNotFoundException("Unknown provider type: " + Pack.UPLOAD_TYPE);
        }

    }

}
