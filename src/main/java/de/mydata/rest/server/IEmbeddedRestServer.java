package de.mydata.rest.server;

/**
 * @author Bjoern Frohberg, MyData GmbH
 */
public interface IEmbeddedRestServer {

    /**
     * server.start, server.join, gracefull server.destroy
     *
     * @throws Exception
     */
    void execute() throws Exception;
}
