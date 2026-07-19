package org.coffeepop.latchac.core.config;

import org.coffeepop.latchac.core.exempt.ExemptionContext;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigParserTest {

    private static final String FULL_YAML = """
            latch:
              half-life-ms: 5000
            checks:
              movement.flight:
                enabled: true
                trip-threshold: 3.5
                action: LOG
              combat.killaura:
                enabled: false
                trip-threshold: 2.0
                action: LOG
            exemptions:
              regions:
                - name: machine-hall
                  world: world
                  min: [-50, 0, -50]
                  max: [50, 320, 50]
              players:
                names: [Steve]
                uuids: ["00000000-0000-0000-0000-000000000009"]
              trusted-tags:
                - tag: fake-player
                  description: "假人授信"
            """;

    @Test
    void 完整配置解析() {
        LatchConfig config = new ConfigParser().parse(FULL_YAML);

        assertEquals(5000, config.halfLifeMillis());
        assertEquals(2, config.checks().size());
        LatchConfig.CheckConfig flight = config.checks().get("movement.flight");
        assertTrue(flight.enabled());
        assertEquals(3.5, flight.tripThreshold());
        assertEquals("LOG", flight.action());

        // 3 类规则：region + player + tag
        assertEquals(3, config.exemptionRules().size());
    }

    @Test
    void 解析出的规则真实可用() {
        LatchConfig config = new ConfigParser().parse(FULL_YAML);
        ExemptionContext inHall = new ExemptionContext(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Alex", "world", 0, 64, 0, "movement.flight", Set.of());
        boolean anyHit = config.exemptionRules().stream()
                .anyMatch(r -> r.exempts(inHall).isPresent());
        assertTrue(anyHit, "机器大厅坐标应命中区域豁免");
    }

    @Test
    void 缺省节回退默认值() {
        LatchConfig config = new ConfigParser().parse("latch: {}\n");
        assertEquals(4000, config.halfLifeMillis());
        assertTrue(config.checks().isEmpty());
        assertTrue(config.exemptionRules().isEmpty());
    }

    @Test
    void 非法YAML抛出异常供调用方保留旧配置() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigParser().parse("just a string"));
    }
}
