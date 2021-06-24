package com.example.salvo.services.implementations;

import com.example.salvo.models.Ship;
import com.example.salvo.services.ShipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShipServiceImplement implements ShipService {
    @Autowired
    ShipService shipService;

    @Override
    public Ship findByType(String type) {
        shipService.findByType(type);
        return null;
    }
}
