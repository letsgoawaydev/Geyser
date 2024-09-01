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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.bedrock.entity.player.auth.BedrockActionAuth;
import org.geysermc.geyser.translator.protocol.bedrock.entity.player.auth.BedrockInputAuth;
import org.geysermc.geyser.translator.protocol.bedrock.entity.player.auth.BedrockMovePlayerAuth;

@Translator(packet = PlayerAuthInputPacket.class)
public class BedrockAuthInputTranslator extends PacketTranslator<PlayerAuthInputPacket> {
    public void translate(GeyserSession session, PlayerAuthInputPacket packet) {
        GeyserImpl.getInstance().getLogger().info(packet.toString());
        SessionPlayerEntity entity = session.getPlayerEntity();
        if (!session.isSpawned()) return;
        BedrockInputAuth.translate(session, packet, entity);
        BedrockMovePlayerAuth.translate(session, packet, entity);
        BedrockActionAuth.translate(session, packet, entity);
    }

    private boolean isInvalidNumber(float val) {
        return Float.isNaN(val) || Float.isInfinite(val);
    }

    private boolean isValidMove(GeyserSession session, Vector3f currentPosition, Vector3f newPosition) {
        if (isInvalidNumber(newPosition.getX()) || isInvalidNumber(newPosition.getY()) || isInvalidNumber(newPosition.getZ())) {
            return false;
        }
        if (currentPosition.distanceSquared(newPosition) > 300) {
            session.getGeyser().getLogger().debug(ChatColor.RED + session.bedrockUsername() + " moved too quickly." +
                    " current position: " + currentPosition + ", new position: " + newPosition);

            return false;
        }

        return true;
    }
}
