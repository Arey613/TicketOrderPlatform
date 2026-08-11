package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.generated.contract.model.EventListScope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
class EventListScopeConverter implements Converter<String, EventListScope> {

  @Override
  public EventListScope convert(String source) {
    return EventListScope.fromValue(source.toLowerCase());
  }
}
