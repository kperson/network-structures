atoVagrant.configure(2) do |config|

  config.vm.box = "tallypix/tallyho"
  config.vm.box_url = "https://s3.amazonaws.com/tallyhovirtualbox/tallyho.json"
  config.ssh.password = "vagrant"
  config.ssh.username = "vagrant"

  config.vm.provider :virtualbox do |p|
    p.name = "structures"
    p.memory = 4096
    p.cpus = 4
    p.customize ["modifyvm", :id, "--cpuexecutioncap", "85"]
  end

  config.vm.hostname = "stuctures.kelt.com"
  config.vm.synced_folder ".", "/vagrant", type: "nfs", nfs_udp: false, :mount_options => ['nolock,noatime']
  config.vm.network "private_network", ip: "172.16.100.101"
  config.vm.network :forwarded_port, guest: 22, host: 2202

end
