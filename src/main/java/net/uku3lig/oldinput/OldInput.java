package net.uku3lig.oldinput;

import com.google.common.util.concurrent.AtomicDouble;
import net.java.games.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Mod(modid = OldInput.MOD_ID, name = OldInput.MOD_NAME, version = OldInput.VERSION)
public class OldInput extends MouseHelper {
    public static final String MOD_ID = "oldinput";
    public static final String MOD_NAME = "OldInput";
    public static final String VERSION = "1.1.3";

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);
    private static final OldinputControllerEnvironment controllerEnv = new OldinputControllerEnvironment();

    private final AtomicDouble dx = new AtomicDouble();
    private final AtomicDouble dy = new AtomicDouble();
    private final AtomicBoolean wasScreen = new AtomicBoolean(false);

    private Set<Mouse> mice = new HashSet<>();

    @Override
    public void mouseXYChange() {
        this.deltaX = (int) dx.getAndSet(0);
        this.deltaY = (int) -dy.getAndSet(0);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Minecraft.getMinecraft().mouseHelper = this;

        executor.scheduleAtFixedRate(this::pollInputs, 0, 1, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::rescanMice, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void pollInputs() {
        if (Minecraft.getMinecraft().currentScreen == null) {
            mice.forEach(mouse -> {
                mouse.poll();
                final float x = mouse.getX().getPollData();
                final float y = mouse.getY().getPollData();
                if(Math.abs(x) > 1.0f) {dx.addAndGet(x);}
                if(Math.abs(y) > 1.0f) {dy.addAndGet(y);}
            });
        }
    }

    private void rescanMice() {
        boolean isScreen = Minecraft.getMinecraft().currentScreen != null;
        // only rescan on the first time we open a screen
        if (isScreen && !wasScreen.get()) {
            controllerEnv.scanControllers();
            this.mice = Arrays.stream(controllerEnv.getControllers()).filter(Mouse.class::isInstance).map(Mouse.class::cast).collect(Collectors.toSet());
        }

        wasScreen.set(isScreen);
    }
}
