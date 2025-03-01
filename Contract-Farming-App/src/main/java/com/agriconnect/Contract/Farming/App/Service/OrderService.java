package com.agriconnect.Contract.Farming.App.Service;

import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
    }

    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }
        return orders;
    }

    public List<Order> getAllOrdersByUser(String userId) {
        List<Order> orders = orderRepository.findOrdersByUser(userId);
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }
        return orders;
    }
}
