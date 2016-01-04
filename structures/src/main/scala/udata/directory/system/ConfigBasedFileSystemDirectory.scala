package udata.directory.system

import java.io.File

import udata.{HubActorSystem, LocalConfig}

class ConfigBasedFileSystemDirectory() extends FileSystemDirectory(new File(new LocalConfig().rootDirectory))