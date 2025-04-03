package dev.lukebemish.codecextras.structured.reflective;

record ParameterizedTypeResults(Class<?> rawType, ReflectiveStructureCreator.TypedCreator[] parameterCreators) {}
