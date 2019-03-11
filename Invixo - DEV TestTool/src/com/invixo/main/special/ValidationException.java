package com.invixo.main.special;

class ValidationException extends Exception {
	private static final long serialVersionUID = 5694583002323453092L;

	ValidationException(String message) {
        super(message);
    }

}