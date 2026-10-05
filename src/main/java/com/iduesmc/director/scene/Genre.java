package com.iduesmc.director.scene;

import com.iduesmc.director.Cinema;

public enum Genre {
	TERROR("TERROR", "Nadie sale vivo del primer acto", 0xAA0000, "El Ente"),
	WESTERN("WESTERN", "Este servidor es demasiado pequeño para los dos", 0xD2A060, "Forajido"),
	ROMANCE("COMEDIA ROMÁNTICA", "Los monstruos solo querían un abrazo", Cinema.PINK, "Amor imposible"),
	MUSICAL("MUSICAL", "Todo el mundo baila. Todo. El. Mundo.", 0x55FF55, "Bailarín de reparto"),
	ANIME("ANIME", "Tu poder ha superado los 9000", 0x5599FF, "Rival-senpai"),
	DOCUMENTAL("DOCUMENTAL DE NATURALEZA", "Observemos al jugador en su hábitat natural", 0x66AA66, "Ejemplar salvaje");

	public final String title;
	public final String tagline;
	public final int color;
	public final String mobName;

	Genre(String title, String tagline, int color, String mobName) {
		this.title = title;
		this.tagline = tagline;
		this.color = color;
		this.mobName = mobName;
	}
}
