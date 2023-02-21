package com.hy.openplant;

import com.magus.net.IOPConnect;
import com.magus.net.OPConnect;
import com.magus.net.OPConnectsFactory;
import com.magus.net.OPSessionConnects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class MagusConnector {
    private static final Logger logger = LoggerFactory.getLogger(MagusConnector.class);

    @Value("${magus.host}")
    private String host;
    @Value("${magus.port}")
    private int port;
    @Value("${magus.user}")
    private String user;
    @Value("${magus.passwd}")
    private String passwd;
    @Value("${magus.maxconn}")
    private int maxconn;
    private OPSessionConnects conns;


    @PostConstruct
    public void init() {
        conns = OPConnectsFactory.getSessionConnects(host, port, user, passwd);
        conns.setMaxConn(maxconn);
    }

    public IOPConnect getConnect() {
        if (conns == null) {
            return null;
        }
        IOPConnect conn = null;
        try {
            conn = conns.getConnect();
            logger.info("get connect success!");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        finally {
//            if (conn != null) {
//                conns.freeConnect(conn);
//            }
//        }

        return conn;
    }

    public void freeConnect(IOPConnect conn) {
        logger.debug("@@@@@freeConnect" + conn);
        conns.freeConnect(conn);
    }

    public void closeConnect(IOPConnect conn) {
        conns.closeConnect(conn);
    }

    public void closeAll() {
        conns.closeAll();
    }
}
