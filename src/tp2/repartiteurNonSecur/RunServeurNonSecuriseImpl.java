package tp2.repartiteurNonSecur;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tp2.shared.ServeurInterface;

/**
 * Classe runnable/ Thread utilisé pour chaque serveur en mode non sécurisé. 
 * @author Enzo Laurent / Victor Pongnian 
 *
 */
public class RunServeurNonSecuriseImpl implements Runnable {
	private ServeurInterface serveurInterface;								//Serveur sur lequel on récupère les données
	private HashMap<String, ArrayList<Integer>> listeOperationThread;		//Liste des opérations que le serveur distant est en train d'effectuer
	private HashMap<String, ArrayList<Integer>> listeOperationVerifThread;	//Liste des opérations que le serveur distant est en train de vérifier
	private int nbrOperations;												//Nombre d'opération à effectuer
	private RepartiteurNonSecurise rpt;										//Répartiteur sur lequel tourne le thread
	private String nom;														//Nom donnée au thread. Typiquement le répartiteur lui donne le même que le serveur auquel il est rattaché	
	
	/**
	 * Constructeur du Thread
	 * 
	 * @param nomThread nom donnée au thread.  
	 * @param serveur Serveur rattaché.
	 * @param rpt Référence du répartiteur qui a lancé le thread.
	 */
	public RunServeurNonSecuriseImpl(String nomThread, ServeurInterface serveur, RepartiteurNonSecurise rpt) {
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
			
			//Tant qu'il y a des serveurs en train de faire des calculs et que tous les paquets de la liste ne sont pas vérifiés
			do
			{
				// Et tant qu'il y a des opérations à traiter dans la liste initial des opérations
				while(rpt.getNbrOperations() > 0){
				
						int resultatCalcul = -1;
						
						listeOperationThread = rpt.getOperations(nbrOperations);
						// Incrémente le nombre de serveur occupé 
						rpt.setServeurOccupe();
						// Envoie la liste des opérations à calculer au serveur
						resultatCalcul = serveurInterface.calculOperations(listeOperationThread);
						
						// Si le calcul a été accepté
						if(resultatCalcul != -1)
						{
							System.out.println("Résultat intermediaire du serveur " + nom + " : " + resultatCalcul);
							// On transfère la liste d'opération que l'on vient de faire dans la liste de Paquet d'opérations 
							// pour que les autres serveurs vérifient le calcul
							rpt.setPaquet(listeOperationThread, nom, resultatCalcul);
							
							//On gère le cas particulier ou le nbrOperations vaut 1
							if(nbrOperations == 1)
							{
								nbrOperations++;
							}
							// Dans les autres cas on augmente la valeur 
							// de nbrOperations de nbrOperations/2 pour simuler 
							// le fait qu'on donne plus de calcul à faire à un serveur disponible.
							else
							{
								nbrOperations = nbrOperations + nbrOperations/2;
							}
						}
						//Sinon on doit remettre les opérations dans la liste d'opération initiale du répartiteur.
						else 
						{
							rpt.resetOperations(listeOperationThread, false);
							// On divise par 2 car on s'est vu refusé un calcul par manque de disponibilité
							nbrOperations = nbrOperations/2;
						}
						
						listeOperationThread = null;
						// Décrémente le nombre de serveur occupé 
						rpt.setServeurInnoccupe();
				}
				
				// On récupère la liste de paquet d'opération
				List<Paquet> listPaquet = rpt.getListPaquet();
				
				// On parcourt la liste de paquet
				for(Paquet pqt : listPaquet) {
					
					/**
					 * Fait le calcul si :
					 * - Ce paquet n'est pas potentiellement vérifiable (il n'y a pas plus de 50% des serveurs qui ont déjà ou qui sont en train de faire le calcul ET si il n'y a pas des serveurs en train de faire le calcul
					 * - Ce serveur n'a pas déjà calculé cette liste d'opération
					 * - Ce paquet n'a pas encore été vérifié 
					 */
					if(!(pqt.verificationEnCours(rpt.getNbrServeurConnecte())) && !(pqt.serveurDejaTraite(nom)) && !(pqt.estVerifie()))
					{
						int resultatCalcul, resultatFinal = -1;
						// On récupère la liste des opération de ce paquet
						listeOperationVerifThread = pqt.getListeOperation();
						// Incrémente le nombre de serveur occupé 
						rpt.setServeurOccupe();
						// Enregistre dans le paquet que ce serveur est en train de faire le calcul 
						pqt.setServeurEnCours(nom);
						// Met à jour le paquet coté répartiteur
						rpt.modifPaquet(pqt);
						
						/**
						 *  Tant que le serveur n'a pas accepté le calcul
						 *  La taille des paquets est fixe, nous attendons que le serveur accepte le calcul
						 */
						do
						{
							// Envoie la liste des opérations à calculer au serveur
							resultatCalcul = serveurInterface.calculOperations(listeOperationVerifThread);
							
							// Si le calcul a été accepté
							if(resultatCalcul != -1)
							{
								System.out.println("Resultat recalculé du serveur " + nom + " : " + resultatCalcul);
								// Supprime le serveur de la liste des serveurs en train de faire le calcul 
								pqt.delServeurEnCours(nom);
								// Enregistre le résultat de ce serveur dans le paquet
								pqt.setResultat(nom, resultatCalcul);
								// Decrémente le nombre de calcul occupé
								rpt.setServeurInnoccupe();
								
								// Si il n'y a pas serveurs en train de faire un calcul de ce paquet d'opération
								if(!(pqt.hasServeursEnCours()))
								{
									// On vérifie si le résultat valide final peut être trouvé
									resultatFinal = pqt.resultatValide(rpt.getNbrServeurConnecte());
									
									// Si le résultat est différent de -1, le résultat est validé
									if(resultatFinal != -1)
									{
										//Alors on enregistre le résultat vérifié auprès du répartiteur.
										rpt.setResultat(resultatFinal);
										System.out.println("Paquet de valeur " + resultatFinal + " vérifié");
									}
								}
								
								// Met à jour le paquet coté répartiteur
								rpt.modifPaquet(pqt);
							}
							
						}
						while (resultatCalcul == -1);
					}
				}
				
			}
			while(rpt.getNbrServeurOccupe() != 0 || !(rpt.listPaquetEstVerifiee()));
			
		} catch (RemoteException e) {
			// e.printStackTrace();
			System.out.println("Connexion perdue");
			// Si on perd la connexion, on remet les opérations dans la liste initiale du répartiteur
			// Si on est dans la phase de vérification, listeOperationThread sera null
			// true en paramètre car nous sommes dans le catch, nbrServeurErreur est incrémenté 
			rpt.resetOperations(listeOperationThread, true);
			// Décrémente le nombre de serveur occupé 
			rpt.setServeurInnoccupe();
			listeOperationThread = null;
			System.out.println("Redistribution du calcul...");
		} 
		
		System.out.println("Fin du thread");
		// On décrémente nbrServeurConnecte
		rpt.deconnectThread();
	}

}
