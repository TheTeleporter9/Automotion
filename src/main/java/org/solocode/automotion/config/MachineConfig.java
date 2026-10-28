package org.solocode.automotion.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MachineSettings {

    private final static MachineSettings instance = new MachineSettings();

    private File configuration;
    private YamlConfiguration config;



    private MachineSettings() {

    }

}
