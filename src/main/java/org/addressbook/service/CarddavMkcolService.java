package org.addressbook.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.nodes.Node;

import java.util.List;

@ApplicationScoped
public class CarddavMkcolService {
    public List<Node> getMkcolProps(List<Node> nodes, String name) {
        return null;
    }

    public boolean mkColCollect(String collectId, String body) {
        return true;
    }
}
