package com.xiaomian124.nationalday.util;

import com.xiaomian124.nationalday.NationalDay;
import org.bukkit.NamespacedKey;

public final class Keys {

    private Keys() {}

    public static final NamespacedKey IS_NATIONAL_FLAG =
            new NamespacedKey(NationalDay.getInstance(), "national_flag");

    public static final NamespacedKey EFFECT_TYPES =
            new NamespacedKey(NationalDay.getInstance(), "effect_types");

    public static final NamespacedKey EFFECT_AMPS =
            new NamespacedKey(NationalDay.getInstance(), "effect_amps");

    public static final NamespacedKey DURATION =
            new NamespacedKey(NationalDay.getInstance(), "duration");

    public static final NamespacedKey RANGE =
            new NamespacedKey(NationalDay.getInstance(), "range");

    public static final NamespacedKey LEVEL_BONUS =
            new NamespacedKey(NationalDay.getInstance(), "level_bonus");

    public static final NamespacedKey ACTIVATED =
            new NamespacedKey(NationalDay.getInstance(), "activated");

    public static final NamespacedKey LOCKED_POTION =
            new NamespacedKey(NationalDay.getInstance(), "locked_potion");
}