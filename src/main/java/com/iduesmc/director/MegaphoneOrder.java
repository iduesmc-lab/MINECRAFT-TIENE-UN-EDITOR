package com.iduesmc.director;

public enum MegaphoneOrder {
	REPLAY("¡REPETICIÓN!"),
	SLOW_MOTION("¡CÁMARA LENTA!"),
	GENRE("¡CAMBIO DE GÉNERO!");

	public final String label;

	MegaphoneOrder(String label) {
		this.label = label;
	}

	public MegaphoneOrder next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
