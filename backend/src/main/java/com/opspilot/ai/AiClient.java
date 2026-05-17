package com.opspilot.ai;

import com.opspilot.ai.dto.AiTriageOutput;
import com.opspilot.ai.dto.AiTriageRequest;

public interface AiClient {
    AiTriageOutput triageTicket(AiTriageRequest request);
}