package org.solocode.automotion.Data;

import org.bukkit.block.Block;

import java.util.List;

public record Machine(
        String id,
        String blockId,
        List<Recipe> recipes
) { }
