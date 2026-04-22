package com.bofa.cbp.wire.web;

import com.bofa.cbp.wire.service.FeeCalculator;
import com.bofa.cbp.wire.service.WireEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal ops endpoints — fee preview + event buffer inspection.
 * Not exercised by unit tests.
 */
@RestController
@RequestMapping("/admin/wires")
public class WireAdminController {

    @Autowired
    private FeeCalculator feeCalculator;

    @Autowired
    private WireEventPublisher publisher;

    @GetMapping("/fee-preview")
    public Map<String, Object> feePreview(@RequestParam String currency,
                                          @RequestParam BigDecimal amount) {
        Map<String, Object> out = new HashMap<>();
        out.put("currency", currency);
        out.put("amount", amount);
        out.put("domestic", feeCalculator.isDomestic(currency));
        out.put("fee", feeCalculator.estimateFee(currency, amount));
        out.put("net", feeCalculator.netAmount(currency, amount));
        return out;
    }

    @GetMapping("/events")
    public List<String> events() {
        return publisher.peek();
    }

    @PostMapping("/events/drain")
    public List<String> drain() {
        return publisher.drain();
    }
}
