package com.prats.paypal.controller;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import com.prats.paypal.service.PaypalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaypalController {

    private final PaypalService paypalService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/payment/create")
    public RedirectView createPayment() {
        try {
            String cancelUrl = "http://localhost:8080/payment/cancel";
            String successUrl = "http://localhost:8080/payment/success";

            Payment payment = paypalService.createPayment(
                    10.00,
                    "USD",
                    "paypal",
                    "sale",
                    "Payment Description",
                    cancelUrl,
                    successUrl
            );
//                1 Option : using For loop
//                for(Links links : payment.getLinks()){
//                    if(links.getRel().equals("approval_url")){
//                        return new RedirectView(links.getHref());
//                    }
//                }
//                2. Option :  using Stream
            return payment.getLinks().stream().filter(link -> "approval_url".equals(link.getRel()))
                    .findFirst()
                    .map(link -> new RedirectView(link.getHref()))
                    .orElseThrow(() -> new RuntimeException("Approval URL not found ⛔️"));

        } catch (PayPalRESTException exception) {
            log.error("Payment creation failed", exception);
            exception.printStackTrace();
        }
        return new RedirectView("/payment/error");
    }

    @GetMapping("/payment/success")
    public String paymentSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerID) {
        try{
            Payment payment = paypalService.executePayment(paymentId, payerID);
            if(payment.getState().equals("approved")) {
                return "paymentSuccess";
            }
        }catch (PayPalRESTException exception){
            log.error("Payment creation failed", exception);
            exception.printStackTrace();
        }
        return "paymentSuccess";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel(){
        return "paymentCancel";
    }

    @GetMapping("/payment/error")
    public String paymentError(){
        return "paymentError";
    }
}
