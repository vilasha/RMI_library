package server;

import common.LibraryServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import static common.LibraryServer.*;

/**
 * Server application. Launches 3 libraries: Concordia (CON), McGill (MCG) and Montreal (MON)
 * each has an separate url:
 *      rmi://localhost:1099/concordia
 *      rmi://localhost:1099/mcgill
 *      rmi://localhost:1099/montreal
 */
public class StartServer {

    public static void main(String[] args) throws RemoteException, MalformedURLException {
        LocateRegistry.createRegistry(PORT);
        LibraryServer server_con = new ServerImpl("CON");
        Naming.rebind(URL_CON, server_con);
        LibraryServer server_mcg = new ServerImpl("MCG");
        Naming.rebind(URL_MCG, server_mcg);
        LibraryServer server_mon = new ServerImpl("MON");
        Naming.rebind(URL_MON, server_mon);
        System.out.println("Waiting for clients...");
    }
}
