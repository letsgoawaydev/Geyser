/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type;

import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.MotionPredictionHintsPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.session.GeyserSession;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AbstractArrowEntity extends Entity implements Tickable {
    private boolean inGround = false;

    public AbstractArrowEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);

        // Set the correct texture if using the resource pack
        setFlag(EntityFlag.BRIBED, definition.entityType() == EntityType.SPECTRAL_ARROW);
        setMotion(motion);
    }

    public void setInGround(BooleanEntityMetadata entityMetadata) {
        inGround = entityMetadata.getPrimitiveValue();

        if (inGround) {
            playEntityEvent(EntityEventType.ARROW_SHAKE, 7);
            setMotion(Vector3f.ZERO);
        }
        // GeyserImpl.getInstance().getLogger().debug("In Ground? " + entityMetadata.getPrimitiveValue());
    }

    public void setArrowFlags(ByteEntityMetadata entityMetadata) {
        byte data = entityMetadata.getPrimitiveValue();

        setFlag(EntityFlag.CRITICAL, (data & 0x01) == 0x01);
    }

    // Ignore the rotation sent by the Java server since the
    // Java client calculates the rotation from the motion
    @Override
    public void setYaw(float yaw) {
    }

    @Override
    public void setPitch(float pitch) {
    }

    @Override
    public void setHeadYaw(float headYaw) {
    }

    @Override
    public void setMotion(Vector3f motion) {
        super.setMotion(motion);
        MotionPredictionHintsPacket motionPacket = new MotionPredictionHintsPacket();
        motionPacket.setMotion(motion);
        motionPacket.setRuntimeEntityId(getGeyserId());
        motionPacket.setOnGround(inGround);
        session.sendUpstreamPacket(motionPacket);
    }

    @Override
    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, boolean isOnGround) {
        motion = motion.add(relX, relY, relZ);
    }

    @Override
    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        motion = motion.add(relX, relY, relZ);
    }

    @Override
    public void tick() {

        if (!inGround) {
            double horizontalSpeed = Math.sqrt(motion.getX() * motion.getX() + motion.getZ() * motion.getZ());
            yaw = ((float) Math.toDegrees(Math.atan2(motion.getX(), motion.getZ())));
            pitch = ((float) Math.toDegrees(Math.atan2(motion.getY(), horizontalSpeed)));
            headYaw = yaw;

            moveAbsolute(position.add(motion), yaw, pitch, headYaw, inGround, false);

            float drag = getDrag();
            float gravity = getGravity();
            motion = motion.mul(drag).down(gravity);
        } else {
            moveAbsolute(position.sub(motion), yaw, pitch, headYaw, inGround, false);
        }
    }


    private float getGravity() {
        if (getFlag(EntityFlag.HAS_GRAVITY) && !inGround) {
            // Gravity can change if the item is in water/lava, but
            // the server calculates the motion & position for us
            return 0.05f;
        }
        return 0.0f;
    }

    /**
     * @return true if this entity is currently in water.
     */
    private boolean isInWater() {
        int block = session.getGeyser().getWorldManager().getBlockAt(session, position.toInt());
        return BlockStateValues.getWaterLevel(block) != -1;
    }

    private float getDrag() {
        if (inGround) {
            return 0.0f;
        }
        if (isInWater()) {
            return 0.6f;
        }
        return 0.99f;
    }
}
