/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.translator.protocol.bedrock.entity.player.auth;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.translator.item.CustomItemTranslator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.geyser.util.CooldownUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

public class BedrockActionAuth {
    public static void translate(GeyserSession session, PlayerAuthInputPacket packet, SessionPlayerEntity entity) {
        // Send book update before any player action
        for (PlayerAuthInputData action : packet.getInputData()) {

            session.getBookEditCache().checkForSend();

            if (packet.getInputData().contains(PlayerAuthInputData.PERFORM_ITEM_INTERACTION)) {
                Vector3i vector = packet.getItemUseTransaction().getBlockPosition();
            }

            switch (action) {
                case START_SWIMMING -> {
                    if (!entity.getFlag(EntityFlag.SWIMMING)) {
                        ServerboundPlayerCommandPacket startSwimPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SPRINTING);
                        session.sendDownstreamGamePacket(startSwimPacket);

                        session.setSwimming(true);
                    }
                }
                case STOP_SWIMMING -> {
                    // Prevent packet spam when Bedrock players are crawling near the edge of a block
                    if (!session.getCollisionManager().mustPlayerCrawlHere()) {
                        ServerboundPlayerCommandPacket stopSwimPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.STOP_SPRINTING);
                        session.sendDownstreamGamePacket(stopSwimPacket);

                        session.setSwimming(false);
                    }
                }
                case START_GLIDING -> {
                    // Otherwise gliding will not work in creative
                    ServerboundPlayerAbilitiesPacket playerAbilitiesPacket = new ServerboundPlayerAbilitiesPacket(false);
                    session.sendDownstreamGamePacket(playerAbilitiesPacket);
                    sendPlayerGlideToggle(session, entity);
                }
                case STOP_GLIDING -> sendPlayerGlideToggle(session, entity);
                case START_SNEAKING -> {
                    ServerboundPlayerCommandPacket startSneakPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SNEAKING);
                    session.sendDownstreamGamePacket(startSneakPacket);

                    session.startSneaking();
                }
                case STOP_SNEAKING -> {
                    ServerboundPlayerCommandPacket stopSneakPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.STOP_SNEAKING);
                    session.sendDownstreamGamePacket(stopSneakPacket);

                    session.stopSneaking();
                }
                case START_SPRINTING -> {
                    if (!entity.getFlag(EntityFlag.SWIMMING)) {
                        ServerboundPlayerCommandPacket startSprintPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SPRINTING);
                        session.sendDownstreamGamePacket(startSprintPacket);
                        session.setSprinting(true);
                    }
                }
                case STOP_SPRINTING -> {
                    if (!entity.getFlag(EntityFlag.SWIMMING)) {
                        ServerboundPlayerCommandPacket stopSprintPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.STOP_SPRINTING);
                        session.sendDownstreamGamePacket(stopSprintPacket);
                    }
                    session.setSprinting(false);
                }
                case MISSED_SWING -> {
                    // Java edition sends a cooldown when hitting air.
                    // Normally handled by BedrockLevelSoundEventTranslator, but there is no sound on Java for this.
                    CooldownUtils.sendCooldown(session);

                    // TODO Re-evaluate after pre-1.20.10 is no longer supported?
                    if (session.getArmAnimationTicks() == -1) {
                        session.sendDownstreamGamePacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
                        session.activateArmAnimationTicking();

                        // Send packet to Bedrock so it knows
                        AnimatePacket animatePacket = new AnimatePacket();
                        animatePacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
                        animatePacket.setAction(AnimatePacket.Action.SWING_ARM);
                        session.sendUpstreamPacket(animatePacket);
                    }
                }
                case START_FLYING -> { // Since 1.20.30
                    if (session.isCanFly()) {
                        if (session.getGameMode() == GameMode.SPECTATOR) {
                            // should already be flying
                            session.sendAdventureSettings();
                            break;
                        }

                        if (session.getPlayerEntity().getFlag(EntityFlag.SWIMMING) && session.getCollisionManager().isPlayerInWater()) {
                            // As of 1.18.1, Java Edition cannot fly while in water, but it can fly while crawling
                            // If this isn't present, swimming on a 1.13.2 server and then attempting to fly will put you into a flying/swimming state that is invalid on JE
                            session.sendAdventureSettings();
                            break;
                        }

                        session.setFlying(true);
                        session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(true));
                    } else {
                        // update whether we can fly
                        session.sendAdventureSettings();
                        // stop flying
                        PlayerActionPacket stopFlyingPacket = new PlayerActionPacket();
                        stopFlyingPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
                        stopFlyingPacket.setAction(PlayerActionType.STOP_FLYING);
                        stopFlyingPacket.setBlockPosition(Vector3i.ZERO);
                        stopFlyingPacket.setResultPosition(Vector3i.ZERO);
                        stopFlyingPacket.setFace(0);
                        session.sendUpstreamPacket(stopFlyingPacket);
                    }
                }
                case STOP_FLYING -> {
                    session.setFlying(false);
                    session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(false));
                }
            }
        }
    }

    private static void spawnBlockBreakParticles(GeyserSession session, Direction direction, Vector3i position, BlockState blockState) {
        LevelEventPacket levelEventPacket = new LevelEventPacket();
        switch (direction) {
            case UP -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_UP);
            case DOWN -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_DOWN);
            case NORTH -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_NORTH);
            case EAST -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_EAST);
            case SOUTH -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_SOUTH);
            case WEST -> levelEventPacket.setType(LevelEvent.PARTICLE_BREAK_BLOCK_WEST);
        }
        levelEventPacket.setPosition(position.toFloat());
        levelEventPacket.setData(session.getBlockMappings().getBedrockBlock(blockState).getRuntimeId());
        session.sendUpstreamPacket(levelEventPacket);
    }

    private static void sendPlayerGlideToggle(GeyserSession session, Entity entity) {
        ServerboundPlayerCommandPacket glidePacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_ELYTRA_FLYING);
        session.sendDownstreamGamePacket(glidePacket);
    }
}
