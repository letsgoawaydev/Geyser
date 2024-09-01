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
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;

public class BedrockInputAuth {
    public static void translate(GeyserSession session, PlayerAuthInputPacket packet, SessionPlayerEntity entity) {
        if (entity.getVehicleInput() != packet.getAnalogMoveVector()) {
            ServerboundPlayerInputPacket playerInputPacket = new ServerboundPlayerInputPacket(
                    packet.getAnalogMoveVector().getX(), packet.getAnalogMoveVector().getY(), packet.getInputData().contains(PlayerAuthInputData.JUMPING), packet.getInputData().contains(PlayerAuthInputData.SNEAKING)
            );

            session.sendDownstreamGamePacket(playerInputPacket);

            entity.setVehicleInput(packet.getAnalogMoveVector());

            // Bedrock only sends movement vehicle packets while moving
            // This allows horses to take damage while standing on magma
            Entity vehicle = entity.getVehicle();
            boolean sendMovement = false;
            if (vehicle instanceof AbstractHorseEntity && !(vehicle instanceof LlamaEntity)) {
                sendMovement = vehicle.isOnGround();
            } else if (vehicle instanceof BoatEntity) {
                if (vehicle.getPassengers().size() == 1) {
                    // The player is the only rider
                    sendMovement = true;
                } else {
                    // Check if the player is the front rider
                    if (entity.isRidingInFront()) {
                        sendMovement = true;
                    }
                }
            }
            if (sendMovement) {
                long timeSinceVehicleMove = System.currentTimeMillis() - session.getLastVehicleMoveTimestamp();
                if (timeSinceVehicleMove >= 100) {
                    Vector3f vehiclePosition = vehicle.getPosition();

                    if (vehicle instanceof BoatEntity && !vehicle.isOnGround()) {
                        // Remove some Y position to prevents boats flying up
                        vehiclePosition = vehiclePosition.down(EntityDefinitions.BOAT.offset());
                    }

                    ServerboundMoveVehiclePacket moveVehiclePacket = new ServerboundMoveVehiclePacket(
                            vehiclePosition.getX(), vehiclePosition.getY(), vehiclePosition.getZ(),
                            vehicle.getYaw() - 90, vehicle.getPitch()
                    );
                    session.sendDownstreamGamePacket(moveVehiclePacket);
                }
            }
        }
    }
}
