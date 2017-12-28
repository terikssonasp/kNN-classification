/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ik2018_inl1;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 *
 * @author Tova Eriksson-Asp och Elin Ekman
 */
public class AnalyticsAgent extends Agent {

    byte[] allDataInByte;
    String[] allLines;
    RConnection connection;

    protected void setup() {

        DFAgentDescription dfd = new DFAgentDescription(); //Beskrivning av agenten
        dfd.setName(getAID());//beskrivning sätts till AID
        ServiceDescription sd = new ServiceDescription();//Beskrivning av tjänsten
        sd.setType("dataForAnalysis");//sätter typen
        sd.setName("JADE-data-analysis");//och namnet

        dfd.addServices(sd); //Sedan kopplar vi tjänsten till beskrvningen av agenten
        try {
            //Med hjälp av register()-metoden registreras denna agent, med tilhörande beskrivning
            //i de gula sidorna
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new DatasetServer());
    }

    private class DatasetServer extends CyclicBehaviour { //letar om dataagenten har lagt ut något på gula sidorna

        @Override
        public void action() {

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM); //kollar om det finns några INFORM-meddelanden
            ACLMessage dataReceivedMsg = myAgent.receive(mt); //laddar in INFORM-meddelanden från mallen

            if (dataReceivedMsg != null) {
                //läser in datat vi skickat med i två variabler
                allDataInByte = dataReceivedMsg.getByteSequenceContent(); //får in en lång bytearray med ca 4000 rader i en enda rad...

                //gör en sträng av bytearrayen och delar upp den i en stringarray
                String str = new String(allDataInByte); //konverterar bytearray till en lång sträng
                allLines = str.split("\n"); //splittar vid varje newline så att en rad blir ett objekt/item i arrayen

                System.out.println(getLocalName() + " har mottagit ett dataset från "
                        + dataReceivedMsg.getSender().getLocalName());

                myAgent.addBehaviour(new DataAnalysis());
            }
        }
    }

    private class DataAnalysis extends OneShotBehaviour {

        @Override
        public void action() {

            connection = null;

            //Denna sträng skall representera vektorn som skall skickas till R
            String vector = null;

            try {
                connection = new RConnection(); //Skapar upp anslutning till Rserve via defaultport 6311

                //Första raden i allLines används som headers
                String[] headers = allLines[0].split(";"); //anpassas efter hur många kolumner det är

                //yttre for-loopen skapar upp vektor för en kolumn, exempelvis citric acid eller residual sugar osv.
                for (int i = 0; i < headers.length; i++) {

                    //dessa två rader fixar till headers, de tar bort citationstecken samt ersätter space med _
                    //vid första varvet 'r det fixed acidity
                    headers[i] = headers[i].replace("\"", ""); //tar bort  citationstecken 
                    headers[i] = headers[i].replace(" ", "_"); //ersätter alla mellanslag med underline 

                    //börjar bygga vectorsträngen genom att ge den ett namn som är samma som headern               
                    vector = headers[i] + "<- c(";

                    try {
                        //inre for-loopen appendar till vektorsträngen, med endast det värde som hör ihop med kolumnen
                        for (int j = 1; j < allLines.length; j++) {
                            //splittar upp varje rad i de olika parametrarna
                            String[] values = allLines[j].split(";");

                            //OBS! values[i] (EJ j) gör att endast det värde som hör ihop med kolumnnamnet läggs till strängen
                            if ((headers[i]).equalsIgnoreCase("target1")) {
                                values[i] = "\"" + values[i] + "\"";
                                values[i] = values[i].replace(" ", "_");
                                values[i] = values[i].toLowerCase();
                            }
                            if (j != allLines.length - 1) {
                                vector = vector + values[i] + ",";
                                //else körs sista varvet i loopen eftersom vi ej vill ha ett kommatecken innan slutparentesen
                            } else {
                                vector = vector + values[i] + ")";
                            }
                        }

                        //System.out.println(vector);
                        
                        //skapar upp vectorn i R
                        connection.eval(vector);
                    } catch (ArrayIndexOutOfBoundsException ae) {
                        continue;
                    }
                }

                //denna kan vi flytta upp i yttre forloopen sedan men lägger den här för tydlighetens skull
                //bygger en sträng som skapar upp vår dataframe
                String createDataFrame = "alldata <- data.frame(";
                for (int k = 0; k < headers.length; k++) {

                    //fullösning för att filerna ser olika ut
                    if (headers[0].equalsIgnoreCase("STG") && k == 5) {
                        createDataFrame = createDataFrame + headers[k] + ")";
                        k = 20;
                    } else if (k != headers.length - 1) {
                        createDataFrame = createDataFrame + headers[k] + ", ";
                    } else {
                        createDataFrame = createDataFrame + headers[k] + ")";
                    }
                }
                connection.eval(createDataFrame);

                connection.eval("kValue<-3\n" //sätt k-värde för kNN
                        + "trainData<-2/3\n" //sätt träningsmängd
                        + "testData<-1/3\n" //sätt testmängd
                        + "kfoldValue<-5"); //sätt k-värde för k-fold

                connection.eval("source('D:/knntilljava.R')"); //använder lokalt r-skript för att göra kNN-beräkning
                
                System.out.printf("\n\n%s\n\n", "CONFUSION MATRIX FÖR kNN-ALGORITMEN PÅ AKUTELL DATASET EFTER DATAVALIDERING MED K-FOLD");
                printFromR("kfoldSum$table");
                separate();
                
                System.out.printf("\n\n%s\n\n", "ACCURACY (m.m.) FÖR kNN-ALGORITMEN PÅ AKUTELL DATASET EFTER DATAVALIDERING MED K-FOLD");
                printFromR("kfoldSum$overall");
                separate();
                
                System.out.printf("\n\n%s\n\n", "PRECISION PER KLASS FÖR kNN-ALGORITMEN PÅ AKUTELL DATASET EFTER DATAVALIDERING MED K-FOLD");
                printFromR("precision"); // skriver ut precision för varje klass
                separate();
                
            } catch (RserveException e) {
                e.printStackTrace();

            } catch (REXPMismatchException ex) {
                Logger.getLogger(AnalyticsAgent.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                connection.close();
            }
        }

        private void printFromR(String s) throws RserveException, REXPMismatchException {
            String output = connection.eval("paste(capture.output(" + s + "), collapse='\\n')").asString();
            System.out.println(output);
        }
        
        private void separate(){
            System.out.println("\n\n========================================================================================================");
        }
    }

}
