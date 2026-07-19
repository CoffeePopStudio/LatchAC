package org.coffeepop.latchac.core.exempt;

import java.util.Optional;

/** 区域豁免：world + AABB 立方体，范围内行为完全免检（四维矩阵之"区域"维）。 */
public record RegionRule(String name, String worldName,
                         double minX, double minY, double minZ,
                         double maxX, double maxY, double maxZ) implements ExemptionRule {

    @Override
    public Optional<String> exempts(ExemptionContext ctx) {
        if (!worldName.equals(ctx.worldName())) {
            return Optional.empty();
        }
        boolean inside = ctx.x() >= minX && ctx.x() <= maxX
                && ctx.y() >= minY && ctx.y() <= maxY
                && ctx.z() >= minZ && ctx.z() <= maxZ;
        return inside ? Optional.of("区域豁免: " + name) : Optional.empty();
    }
}
