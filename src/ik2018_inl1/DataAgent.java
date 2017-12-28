package ik2018_inl1;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tova Eriksson-Asp och Elin Ekman
 */
public class DataAgent extends Agent {

    private byte[] allItemsByte;
    DFAgentDescription[] matchedAgents;
    
    @Override
    protected void setup() {
        try {
            allItemsByte = readDataFromCsv();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DataAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DataAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        addBehaviour(new CheckForAnalyticsAgent());

    }

    private byte[] readDataFromCsv() throws FileNotFoundException, IOException {
        
        Object [] args = getArguments();
        String fileName = (String) args[0]; //vill byta ut till getarguments
        Path path = Paths.get(fileName);
        byte[] allItems = Files.readAllBytes(path);
        return allItems;
    }

    /**
     * Inner class CheckForAnalyticsAgent. Kollar om det finns någon analysagent
     * tillgänglig
     */
    private class CheckForAnalyticsAgent extends OneShotBehaviour {

        @Override
        public void action() {
            //Agentbeskrivning
            DFAgentDescription template = new DFAgentDescription();
            //Servicebeskrivning                                     
            ServiceDescription sd = new ServiceDescription();

            sd.setType("dataForAnalysis");
            //Sedan kopplat vi tjänstebeskrivningen till agentbeskrivningen
            template.addServices(sd);
            try {
                //Vi skapar upp en array av agentbeskrivningar genom att söka efter agentbeskrivningar
                //som motsvarar den vi skapat upp i vårt agentbeskrivningsobjekt template                                            
                matchedAgents = DFService.search(myAgent, template);
                //matchedAgentsAID = new AID[matchedAgents.length];                                   
                //Skriver ut till användaren vilka agenter som hittas
                if (matchedAgents.length > 0) {
                    System.out.println(getLocalName() + " hittar följande agenter som kan analysera data:");
                    for (int i = 0; i < matchedAgents.length; ++i) {
                        System.out.println(matchedAgents[i].getName().getName());
                        //matchedAgentsAID[i] = matchedAgents[i].getName();

                        myAgent.addBehaviour(new SendData());
                    }

                } else {
                    System.out.println(getLocalName() + "hittar inga agenter som kan analysera data.");
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }

    private class SendData extends Behaviour {

        private MessageTemplate mt;

        @Override
        public void action() {
            ACLMessage datasetMsg = new ACLMessage(ACLMessage.INFORM);

            for (int i = 0; i < matchedAgents.length; i++) {
                //metoden getName() hämtar AID från aktuell agent i matchedAgent-arrayen
                //vi har kommenterat bort AID-arrayen, men den kan också användas om man vill
                datasetMsg.addReceiver(matchedAgents[i].getName());
            }

            datasetMsg.setByteSequenceContent(allItemsByte);
            datasetMsg.setConversationId("sendDataConversation");
            myAgent.send(datasetMsg);

        }

        @Override
        public boolean done() {
            return true;
        }

    }
}
