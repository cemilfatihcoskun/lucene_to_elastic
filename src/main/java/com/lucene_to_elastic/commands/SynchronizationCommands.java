package com.lucene_to_elastic.commands;

import java.io.IOException;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.lucene_to_elastic.services.SynchronizationService;

@ShellComponent
public class SynchronizationCommands {
    private final SynchronizationService synchronizationService;
    
    public SynchronizationCommands(SynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }
    
    @ShellMethod("Clean")
    public void synchronizationClean() throws IOException {
        synchronizationService.clean();
    }
}
