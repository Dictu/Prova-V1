/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.dictu.prova.plugins.output;

import java.util.Properties;

/**
 *
 * @author Coos van der Galiën
 */
public interface DbOutputPlugin extends OutputPlugin{
    public Properties doProcessDbResponse () throws Exception;
    
    public void doSetQuery (String query) throws Exception;
    
    public void doSetDbProperties (String adress, String user, String password, Boolean rollback) throws Exception;
    
    public void doSetTests (Properties tests) throws Exception;
}
