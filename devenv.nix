{ pkgs, lib, config, inputs, ... }:

{
  name = "zio-fire-redis";
  languages.java.jdk.package = pkgs.jdk23_headless;
  languages.scala = {
    enable = true;
    sbt.enable = true;
  };

  packages = [ 
    pkgs.git 
    pkgs.redis
  ];

  env.REDIS_URI = "redis://127.0.0.1:6379";
  env.VALKEY_URI = "redis://127.0.0.1:6380";

  enterShell = ''
    echo "~~~ zio-fire-refic ~~~"
  '';

  enterTest = ''
    sbt test
  '';
}
