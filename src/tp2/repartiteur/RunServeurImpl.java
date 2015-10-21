package tp2.repartiteur;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

import tp2.shared.ServeurInterface;

/**
 * Classe runnable/ Thread utilisé pour chaque serveur. 
 * @author Enzo Laurent / Victor Pongnian 
 *
 */
public class RunServeurImpl implements Runnable {
	private ServeurInterface serveurInterface;                          //Serveur sur lequel on récupère les données.
	private HashMap<String, ArrayList<Integer>> listeOperationThread;   //Liste des opérations que le serveur distant est en train d'effectuer
	private int nbrOperations;                                          //Nombre d'opération à effectuer
	private Repartiteur rpt;                                            //Répartiteur sur lequel tourne le thread
	private String nom;                                                 //Nom donnée au thread. Typiquement le répartiteur lui donne le même que le serveur auquel il est rattaché	
	/**
	 * Constructeur du Thread
	 * 
	 * @param nomThread nom donnée au thread.  
	 * @param serveur Serveur rattaché.
	 * @param rpt Référence du répartiteur qui a lancé le thread.
	 */
	public RunServeurImpl(String nomThread, ServeurInterface serveur, Repartiteur rpt) {
		this.nom = nomThread;
		this.serveurInterface = serveur;
		this.rpt = rpt;
	}

	/**
	 * Méthode déclenchée par le Thread.start() dans le répartiteur 
	 */
	public void run() {
		
		// Nombre de bloc initialement donné à calculer au serveur. 
		nbrOperations = rpt.getNbrOperationsInitial()/rpt.getNbrServeurInitial();
		
		try {
			
			do //Tant qu'il y a des serveurs occupés.
			{
				// Et tant qu'il y a des opérations à traiter
				while(rpt.getNbrOperations() > 0){  
				
						int resultatCalcul = -1;
						
						listeOperationThread = rpt.getOperations(nbrOperations);
						
						//Envoie la liste d'opération a calculer au serveur grace à la méthode accessible a distance.
						resultatCalcul = serveurInterface.calculOperations(listeOperationThread);
						
						//Si on a accepté le calcul
						if(resultatCalcul != -1)
						{
							//Alors on enregistre le résultat auprès du répartiteur.
							rpt.setResultat(resultatCalcul);
							System.out.println("Resultat intermediaire du serveur " + nom + " : " + resultatCalcul);
							
							//On gère le cas particulier ou le nbrOperations vaut 1
							if(nbrOperations == 1)
							{
								nbrOperations++;
							}
							// Dans les autres cas on augmente la valeur 
							// de nbrOperations de nbrOperations/2 pour simuler 
							// le fait qu'on donne plus de calcul a faire à un serveur disponible.
							else
							{
								nbrOperations = nbrOperations + nbrOperations/2;
							}
						}
						//Sinon on doit remettre les opérations dans la liste d'opération du répartiteur.
						else 
						{
							rpt.resetOperations(listeOperationThread, false);
							listeOperationThread = null;
							nbrOperations = nbrOperations/2; // On divise par 2 car on s'est vu refusé un calcul par 
						}									 // manque de disponibilité.
				}
			}
			while(rpt.getNbrServeurOccupe() > 0);
			
		} catch (RemoteException e) {                         //Si on perd la connexion, on remet les 
			// e.printStackTrace();                           //opérations dans la liste globale du répartiteur
			System.out.println("Connexion perdue");
			rpt.resetOperations(listeOperationThread, true);
			listeOperationThread = null;
			System.out.println("Redistribution du calcul...");// Et on redistribue les calculs.
		} 
		
		System.out.println("Fin du thread");
		rpt.deconnectThread();                                // Enfin on décrémente le nombre de serveur connecté.
	}

}
