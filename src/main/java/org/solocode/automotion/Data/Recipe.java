package org.solocode.automotion.Data;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public record Recipe(
        String id,
        List<ItemStack> recipeItems,
        List<ItemStack> resultItems
        )
{}
