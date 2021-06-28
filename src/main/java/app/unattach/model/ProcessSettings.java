package app.unattach.model;

import java.io.File;

public record ProcessSettings(ProcessOption processOption, File targetDirectory,
                              String filenameSchema, boolean addMetadata) {}
