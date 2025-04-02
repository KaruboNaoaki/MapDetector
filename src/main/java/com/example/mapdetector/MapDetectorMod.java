package com.example.mapdetector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("mapdetector")
public class MapDetectorMod {
    private static final int MAP_THRESHOLD = 6;
    private boolean notificationShown = false;
    private int cooldownTimer = 0;
    private static final int COOLDOWN_DURATION = 60; // Ticks (3 seconds at 20 ticks per second)
    
    // Screen flash variables
    private boolean isFlashing = false;
    private int flashTimer = 0;
    private static final int FLASH_DURATION = 10; // How long each flash lasts in ticks
    private static final int FLASH_COUNT = 6; // Number of flashes
    private int currentFlash = 0;

    public MapDetectorMod() {
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Only run on client side and only once per tick
            if (event.player.level().isClientSide()) {
                // Handle flash timer
                if (isFlashing) {
                    flashTimer--;
                    if (flashTimer <= 0) {
                        // Toggle flash visibility
                        currentFlash++;
                        if (currentFlash >= FLASH_COUNT) {
                            isFlashing = false;
                            currentFlash = 0;
                        } else {
                            flashTimer = FLASH_DURATION;
                        }
                    }
                }
                
                // Handle cooldown timer
                if (cooldownTimer > 0) {
                    cooldownTimer--;
                    return;
                }

                // Check hotbar for maps
                checkHotbarForMaps(event.player);
            }
        }
    }
    
    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (isFlashing && currentFlash % 2 == 0) {
            // Only render on even flash counts to create flashing effect
            Minecraft minecraft = Minecraft.getInstance();
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();
            
            // Draw red overlay with some transparency
            GuiGraphics guiGraphics = event.getGuiGraphics();
            guiGraphics.fill(0, 0, width, height, 0x77FF0000);  // ARGB: semi-transparent red
            
            // Draw warning text
            Component warningText = Component.literal("§c§lTOO MANY MAPS!");
            int textWidth = minecraft.font.width(warningText);
            guiGraphics.drawString(minecraft.font, warningText, (width - textWidth) / 2, height / 4, 0xFFFFFF);
        }
    }

    private void checkHotbarForMaps(Player player) {
        int mapCount = 0;
        
        // Check each hotbar slot (0-8) for maps
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.FILLED_MAP || stack.getItem() == Items.MAP) {
                mapCount += stack.getCount();
            }
        }

        // Alert player if they have 6 or more maps
        if (mapCount >= MAP_THRESHOLD) {
            if (!notificationShown) {
                // Send chat message
                player.sendSystemMessage(Component.literal("§6You have " + mapCount + " maps in your hotbar!"));
                
                // Play loud alarm sound
                Minecraft minecraft = Minecraft.getInstance();
                float volume = 1.0F;  // Maximum volume
                float pitch = 0.5F;   // Lower pitch for more alarm-like sound
                
                // Play multiple sounds for emphasis
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GOAT_HORN_SOUND_7, volume, pitch));
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_SONIC_BOOM, volume * 0.7F, pitch * 1.5F));
                
                // Start screen flashing
                isFlashing = true;
                flashTimer = FLASH_DURATION;
                currentFlash = 0;
                
                // Set notification state
                notificationShown = true;
                cooldownTimer = COOLDOWN_DURATION;
            }
        } else {
            notificationShown = false;
        }
    }
    
    // Alternative method that can shut down the game if uncommented
    /*
    private void shutdownGame() {
        // Give a short warning before shutdown
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            try {
                // Display final warning
                minecraft.setScreen(null); // Clear any open screens
                minecraft.player.sendSystemMessage(Component.literal("§4§lWARNING: Too many maps detected! Game shutting down in 3 seconds..."));
                
                // Wait 3 seconds
                Thread.sleep(3000);
                
                // Exit game
                minecraft.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    */
}
