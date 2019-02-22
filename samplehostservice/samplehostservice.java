package samplehostservice;


import config.ConfigService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onosproject.grpc.grpcintegration.models.ControlMessagesProto.Empty;
import org.onosproject.grpc.grpcintegration.models.ControlMessagesProto.HostCountProto;
import org.onosproject.grpc.grpcintegration.models.ControlMessagesProto.Hosts;
import org.onosproject.grpc.grpcintegration.models.HostServiceGrpc;
import org.onosproject.grpc.grpcintegration.models.HostServiceGrpc.HostServiceStub;
import org.onosproject.grpc.grpcintegration.models.TopoServiceGrpc;
import org.onosproject.grpc.grpcintegration.models.TopoServiceGrpc.TopoServiceStub;
import org.onosproject.grpc.net.models.DeviceIdProtoOuterClass.DeviceIdProto;
import org.onosproject.grpc.net.models.HostProtoOuterClass.HostProto;
import org.onosproject.grpc.net.topology.models.TopologyGraphProtoOuterClass.TopologyGraphProto;
import org.onosproject.grpc.net.topology.models.TopologyVertexProtoOuterClass.TopologyVertexProto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A sample application to show the usage of hostService.
 */
public class samplehostservice {
  private static Logger log = Logger.getLogger(samplehostservice.class);

  static Set<String> deviceIdSet = new HashSet<>();


  public static void main(String[] args) {

    ManagedChannel channel;
    String controllerIP;
    String grpcPort;

    // Initialize a remote config service for logging
    ConfigService configService = new ConfigService();
    configService.init();
    controllerIP = configService.getConfig().getControllerIp();
    grpcPort = configService.getConfig().getGrpcPort();
    HostServiceStub hostServiceStub;
    TopoServiceStub topologyServiceStub;



    // Creates a gRPC channel
    channel =
        ManagedChannelBuilder
                .forAddress(controllerIP, Integer.parseInt(grpcPort))
                .usePlaintext()
                .build();

    // Creates hostService anf topoService stubs and assigns them to the gRPC channel.

    hostServiceStub = HostServiceGrpc.newStub(channel);
    topologyServiceStub = TopoServiceGrpc.newStub(channel);

    Empty empty = Empty.newBuilder().build();

    DeviceIdProto.Builder deviceIdProtoBuilder = DeviceIdProto
            .newBuilder();



    // Retrieves list of hosts in the network topology
    hostServiceStub.getHosts(empty, new StreamObserver<Hosts>() {
        @Override
        public void onNext(Hosts value) {
            for(HostProto hostProto:value.getHostList()) {

                log.info(hostProto.getIpAddresses(0)
                + ";" + hostProto.getHostId().getMac() + ";" +
                hostProto.getLocation().getConnectPoint().getDeviceId()
                + ":" + hostProto.getLocation().getConnectPoint().getPortNumber());
            }
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
    });

    // Returns number of hosts in the network topology.
    hostServiceStub.getHostCount(empty, new StreamObserver<HostCountProto>() {
        @Override
        public void onNext(HostCountProto value) {
            log.info("Number of Hosts:" + value.getCount());
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
    });

      // Retrieves topology graph and prints the list of hosts connected to each device.
      topologyServiceStub.getGraph(empty,
              new StreamObserver<TopologyGraphProto>() {
          @Override
          public void onNext(TopologyGraphProto value) {


              for(TopologyVertexProto topologyVertexProto: value.getVertexesList()) {

                  DeviceIdProto deviceIdProto = DeviceIdProto
                          .newBuilder()
                          .setDeviceId(topologyVertexProto.getDeviceId().getDeviceId())
                          .build();

                  hostServiceStub.getConnectedHostsByDeviceId(deviceIdProto,
                          new StreamObserver<Hosts>() {
                              @Override
                              public void onNext(Hosts value) {
                                  List<HostProto> hostProtoList = value.getHostList();
                                  for(HostProto hostProto: hostProtoList) {
                                      log.info(deviceIdProto.getDeviceId()
                                              + ":" + hostProto.getHostId().getMac());
                                  }
                              }

                              @Override
                              public void onError(Throwable t) {}

                              @Override
                              public void onCompleted() {}
                          });
              }

          }

          @Override
          public void onError(Throwable t) {}

          @Override
          public void onCompleted() {}
      });



    while(true) {
    }
  }
}
