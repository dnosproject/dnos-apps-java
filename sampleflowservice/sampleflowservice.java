package sampleflowservice;

import config.ConfigService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;
import org.onlab.packet.Ethernet;
import org.onosproject.grpc.grpcintegration.models.ControlMessagesProto.FlowRuleCount;
import org.onosproject.grpc.grpcintegration.models.ControlMessagesProto.FlowRules;
import org.onosproject.grpc.grpcintegration.models.ControlMessagesProto.Empty;
import org.onosproject.grpc.grpcintegration.models.FlowServiceGrpc;
import org.onosproject.grpc.grpcintegration.models.FlowServiceGrpc.FlowServiceStub;
import org.onosproject.grpc.grpcintegration.models.StatusProto.FlowServiceStatus;
import org.onosproject.grpc.grpcintegration.models.TopoServiceGrpc;
import org.onosproject.grpc.grpcintegration.models.TopoServiceGrpc.TopoServiceStub;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass.TypeProto;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass.CriterionProto;
import org.onosproject.grpc.net.flow.criteria.models.CriterionProtoOuterClass.EthTypeCriterionProto;
import org.onosproject.grpc.net.flow.instructions.models.InstructionProtoOuterClass.OutputInstructionProto;
import org.onosproject.grpc.net.flow.instructions.models.InstructionProtoOuterClass.InstructionProto;
import org.onosproject.grpc.net.flow.models.FlowRuleProto;
import org.onosproject.grpc.net.flow.models.TrafficSelectorProtoOuterClass.TrafficSelectorProto;
import org.onosproject.grpc.net.flow.models.TrafficTreatmentProtoOuterClass.TrafficTreatmentProto;
import org.onosproject.grpc.net.models.PortProtoOuterClass;
import org.onosproject.grpc.net.topology.models.TopologyGraphProtoOuterClass.TopologyGraphProto;
import org.onosproject.grpc.net.topology.models.TopologyVertexProtoOuterClass.TopologyVertexProto;

import java.util.List;

public class sampleflowservice {
  private static Logger log = Logger.getLogger(sampleflowservice.class);

  private static int TABLE_ID = 0;
  private static int TABLE_ID_CTRL_PACKETS = 0;
  private static int CTRL_PACKET_PRIORITY = 100;

  private static List<TopologyVertexProto> topologyVertexProtoList;
  private  static FlowRules.Builder flowRulesBuilder = FlowRules.newBuilder();

  public static void main(String[] args) {
    ManagedChannel channel;
    final String CONTROLLER_PORT = "CONTROLLER";
    String controllerIP;
    String grpcPort;

    // Initialize a remote config service for logging
    ConfigService configService = new ConfigService();
    configService.init();
    controllerIP = configService.getConfig().getControllerIp();
    grpcPort = configService.getConfig().getGrpcPort();

    FlowServiceStub flowServiceStub;
    TopoServiceStub topoServiceStub;


    // Creates a gRPC channel.
    channel =
        ManagedChannelBuilder.forAddress(controllerIP, Integer.parseInt(grpcPort))
            .usePlaintext()
            .build();

    // Assigns flowService and topoService stubs to the gRPC channel.
    flowServiceStub = FlowServiceGrpc.newStub(channel);
    topoServiceStub = TopoServiceGrpc.newStub(channel);

    Empty empty = Empty.newBuilder().build();

    // Returns the current topology graph.
    topoServiceStub.getGraph(
        empty, new StreamObserver<TopologyGraphProto>() {

          @Override
          public void onNext(TopologyGraphProto value) {

            topologyVertexProtoList = value.getVertexesList();

            for (TopologyVertexProto topologyVertexProto : topologyVertexProtoList) {

              EthTypeCriterionProto ethTypeCriterionProto =
                  EthTypeCriterionProto.newBuilder()
                          .setEthType(Ethernet.TYPE_IPV4)
                          .build();

              CriterionProto criterionProto =
                  CriterionProto.newBuilder()
                      .setEthTypeCriterion(ethTypeCriterionProto)
                      .setType(TypeProto.ETH_TYPE)
                      .build();

              TrafficSelectorProto trafficSelectorProto =
                  TrafficSelectorProto.newBuilder().addCriterion(criterionProto).build();

              InstructionProto instructionProto =
                  InstructionProto.newBuilder()
                      .setOutput(
                          OutputInstructionProto.newBuilder()
                              .setPort(
                                  PortProtoOuterClass.PortProto.newBuilder()
                                      .setPortNumber(CONTROLLER_PORT)
                                      .build())
                              .build())
                      .build();

              TrafficTreatmentProto trafficTreatmentProto =
                  TrafficTreatmentProto.newBuilder().addAllInstructions(instructionProto).build();

              FlowRuleProto flowRuleProto =
                  FlowRuleProto.newBuilder()
                      .setTreatment(trafficTreatmentProto)
                      .setSelector(trafficSelectorProto)
                      .setPriority(CTRL_PACKET_PRIORITY)
                      .setDeviceId(topologyVertexProto.getDeviceId().getDeviceId())
                      .setTableId(TABLE_ID_CTRL_PACKETS)
                      .setTimeout(0)
                      .setPermanent(true)
                      .setAppName("sampleflowservice")
                      .build();

              // Creates a list of flow rules
              flowRulesBuilder.addFlowrule(flowRuleProto);
            }

            flowServiceStub.applyFlowRules(
                flowRulesBuilder.build(),
                new StreamObserver<FlowServiceStatus>() {
                  @Override
                  public void onNext(FlowServiceStatus value) {
                    if (value.getStat() == true) {
                      log.info("Flow Rules have been installed successfully");
                    }
                  }

                  @Override
                  public void onError(Throwable t) {}

                  @Override
                  public void onCompleted() {}
                });
          }

          @Override
          public void onError(Throwable t) {}

          @Override
          public void onCompleted() {}
        });



      try {
          Thread.sleep(10000);
      } catch (InterruptedException e) {
          e.printStackTrace();
      }

      flowServiceStub.removeFlowRules(flowRulesBuilder.build(),
              new StreamObserver<FlowServiceStatus>() {
          @Override
          public void onNext(FlowServiceStatus value) {
              log.info("flow rules have been removed");
          }

          @Override
          public void onError(Throwable t) {

          }

          @Override
          public void onCompleted() {

          }
      });





      while (true) {}
  }
}
