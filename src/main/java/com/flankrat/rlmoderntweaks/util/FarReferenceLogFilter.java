package com.flankrat.rlmoderntweaks.util;

import com.flankrat.rlmoderntweaks.RLModernTweaks;
import com.flankrat.rlmoderntweaks.feature.OversizedStructures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

/**
 * Hides vanilla's "Found invalid structure reference" warning for references that
 * {@link OversizedStructures} deliberately keeps (listed structures, within the configured
 * radius). Vanilla logs it before {@code mixin.ChunkSerializerMixin} puts the reference back.
 * The warning still shows for every other structure and for anything beyond the radius.
 */
public final class FarReferenceLogFilter extends AbstractFilter {

    private static final String FORMAT = "Found invalid structure reference [ {} @ {} ] for chunk {}.";

    private FarReferenceLogFilter() {
        super(Filter.Result.NEUTRAL, Filter.Result.NEUTRAL);
    }

    public static void install() {
        try {
            if (LogManager.getContext(false) instanceof LoggerContext context) {
                context.getConfiguration().addFilter(new FarReferenceLogFilter());
                context.updateLoggers();
            }
        } catch (Throwable t) {
            RLModernTweaks.LOGGER.warn("Couldn't install the structure reference log filter", t);
        }
    }

    private static Result check(String format, Object[] params) {
        if (!FORMAT.equals(format) || params == null || params.length < 3) return Result.NEUTRAL;
        int radius = OversizedStructures.activeRadius();
        if (radius <= 8) return Result.NEUTRAL;
        if (params[0] instanceof ResourceLocation id && OversizedStructures.isListed(id)
                && params[1] instanceof ChunkPos start && params[2] instanceof ChunkPos chunk
                && start.getChessboardDistance(chunk) <= radius) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return check(msg, params);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return msg == null ? Result.NEUTRAL : check(msg.getFormat(), msg.getParameters());
    }

    @Override
    public Result filter(LogEvent event) {
        Message msg = event.getMessage();
        return msg == null ? Result.NEUTRAL : check(msg.getFormat(), msg.getParameters());
    }
}
