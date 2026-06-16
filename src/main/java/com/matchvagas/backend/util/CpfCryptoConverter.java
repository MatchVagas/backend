package com.matchvagas.backend.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte o CPF transparentemente: cifra ao gravar e decifra ao ler (LGPD-04).
 * Assim, o restante da aplicação continua trabalhando com o CPF em texto puro.
 */
@Converter
public class CpfCryptoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return CpfCrypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return CpfCrypto.decrypt(dbData);
    }
}
