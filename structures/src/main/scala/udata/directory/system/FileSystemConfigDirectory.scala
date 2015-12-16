package udata.directory.system

import java.io.File

import udata.{HubActorSystem, LocalConfig}

class FileSystemConfigDirectory() extends FileSystemDirectory(new File(new LocalConfig().rootDirectory))(HubActorSystem.system.dispatcher)