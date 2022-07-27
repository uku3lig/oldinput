package net.uku3lig.oldinput;

import com.google.common.util.concurrent.AtomicDouble;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(modid = OldInput.MOD_ID, name = OldInput.MOD_NAME, version = OldInput.VERSION)
public class OldInput extends MouseHelper {
    public static final String MOD_ID = "oldinput";
    public static final String MOD_NAME = "OldInput";
    public static final String VERSION = "1.0.0";

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final AtomicDouble dx = new AtomicDouble();
    private final AtomicDouble dy = new AtomicDouble();

    @Override
    public void mouseXYChange() {
        this.deltaX = (int) dx.getAndSet(0);
        this.deltaY = (int) -dy.getAndSet(0);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ControllerEnvironment env = ControllerEnvironment.getDefaultEnvironment();
        Minecraft.getMinecraft().mouseHelper = this;

        executor.scheduleAtFixedRate(() -> {
            if (Minecraft.getMinecraft().currentScreen != null) return;
            Arrays.stream(env.getControllers())
                    .filter(Mouse.class::isInstance)
                    .map(Mouse.class::cast)
                    .forEach(mouse -> {
                        mouse.poll();
                        dx.addAndGet(mouse.getX().getPollData());
                        dy.addAndGet(mouse.getY().getPollData());
                    });
        }, 0, 1, TimeUnit.MILLISECONDS);
    }
}
