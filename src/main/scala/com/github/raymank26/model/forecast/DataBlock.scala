package com.github.raymank26.model.forecast

/**
 * @author Anton Ermak (ermak@yamoney.ru).
 */
case class DataBlock(summary: String, icon: DataPoint.IconType.Icon, data: Seq[DataPoint])
