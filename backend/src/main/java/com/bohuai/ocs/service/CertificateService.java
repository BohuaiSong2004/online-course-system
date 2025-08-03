package com.bohuai.ocs.service;

import com.bohuai.ocs.common.ApiException;
import com.bohuai.ocs.mapper.CertificateMapper;
import com.bohuai.ocs.model.Certificate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

/** Completion certificates with short public verification codes. */
@Service
public class CertificateService {

    // No 0/O, 1/I/L: codes are typed in by people verifying a certificate.
    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateMapper certificates;

    public CertificateService(CertificateMapper certificates) {
        this.certificates = certificates;
    }

    /** Issues the certificate if missing and returns its code. Safe to call repeatedly. */
    public String issue(long userId, long courseId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            certificates.issue(userId, courseId, newCode());
            Certificate c = certificates.findForUserCourse(userId, courseId);
            if (c != null) {
                return c.getCode();
            }
            // INSERT IGNORE also skips on the (astronomically rare) code collision: try a new code.
        }
        throw new IllegalStateException("Could not issue certificate");
    }

    public String codeFor(long userId, long courseId) {
        Certificate c = certificates.findForUserCourse(userId, courseId);
        return c == null ? null : c.getCode();
    }

    public Certificate verify(String code) {
        Certificate c = code == null ? null : certificates.findByCode(code.trim().toUpperCase());
        if (c == null) {
            throw ApiException.notFound("Certificate");
        }
        return c;
    }

    public List<Certificate> mine(long userId) {
        return certificates.listForUser(userId);
    }

    static String newCode() {
        char[] out = new char[12];
        for (int i = 0; i < out.length; i++) {
            out[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }
}
