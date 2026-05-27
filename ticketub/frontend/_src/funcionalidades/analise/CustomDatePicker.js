import React, { useState, useEffect, useRef } from 'react';

export default function CustomDatePicker({ value, label, onChange }) {
  const [isOpen, setIsOpen] = useState(false);
  
  const parseDate = (str) => {
    if (!str) return new Date();
    const [y, m, d] = str.split('-').map(Number);
    return new Date(y, m - 1, d);
  };

  const currentDate = parseDate(value);
  const [viewYear, setViewYear] = useState(currentDate.getFullYear());
  const [viewMonth, setViewMonth] = useState(currentDate.getMonth());

  const containerRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    const d = parseDate(value);
    setViewYear(d.getFullYear());
    setViewMonth(d.getMonth());
  }, [value]);

  const monthNames = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  const currentYear = new Date().getFullYear();
  const years = [];
  for (let y = currentYear - 15; y <= currentYear + 5; y++) {
    years.push(y);
  }

  const changeMonth = (offset) => {
    let newMonth = viewMonth + offset;
    let newYear = viewYear;
    if (newMonth < 0) {
      newMonth = 11;
      newYear -= 1;
    } else if (newMonth > 11) {
      newMonth = 0;
      newYear += 1;
    }
    setViewMonth(newMonth);
    setViewYear(newYear);
  };

  const selectDay = (day) => {
    const pad = (num) => String(num).padStart(2, '0');
    const dateStr = `${viewYear}-${pad(viewMonth + 1)}-${pad(day)}`;
    onChange(dateStr);
    setIsOpen(false);
  };

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDayIndex = new Date(viewYear, viewMonth, 1).getDay();

  const prevMonthDaysToShow = [];
  if (firstDayIndex > 0) {
    const prevMonth = viewMonth === 0 ? 11 : viewMonth - 1;
    const prevYear = viewMonth === 0 ? viewYear - 1 : viewYear;
    const daysInPrevMonth = new Date(prevYear, prevMonth + 1, 0).getDate();
    for (let i = firstDayIndex - 1; i >= 0; i--) {
      prevMonthDaysToShow.push(daysInPrevMonth - i);
    }
  }

  const currentMonthDays = [];
  for (let d = 1; d <= daysInMonth; d++) {
    currentMonthDays.push(d);
  }

  const nextMonthDaysToShow = [];
  const totalSlots = 42;
  const remainingSlots = totalSlots - (prevMonthDaysToShow.length + currentMonthDays.length);
  for (let d = 1; d <= remainingSlots; d++) {
    nextMonthDaysToShow.push(d);
  }

  const formatDateDisplay = (dateStr) => {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('-');
    return `${d}/${m}/${y}`;
  };

  return (
    <div className="custom-datepicker-container" ref={containerRef}>
      {label && <label className="custom-datepicker-label">{label}</label>}
      <div className="custom-datepicker-input-wrapper" onClick={() => setIsOpen(!isOpen)}>
        <span className="custom-datepicker-value">{formatDateDisplay(value)}</span>
        <svg className="custom-datepicker-icon" viewBox="0 0 24 24" width="16" height="16">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2" fill="none" stroke="currentColor" strokeWidth="2"/>
          <line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" strokeWidth="2"/>
          <line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" strokeWidth="2"/>
          <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" strokeWidth="2"/>
        </svg>
      </div>

      {isOpen && (
        <div className="custom-datepicker-popover">
          <div className="custom-datepicker-header">
            <button type="button" className="datepicker-nav-btn" onClick={() => changeMonth(-1)}>
              &larr;
            </button>
            <div className="datepicker-selectors">
              <select
                className="datepicker-select datepicker-select-month"
                value={viewMonth}
                onChange={(e) => setViewMonth(Number(e.target.value))}
              >
                {monthNames.map((name, idx) => (
                  <option key={idx} value={idx}>{name}</option>
                ))}
              </select>
              
              <select
                className="datepicker-select datepicker-select-year"
                value={viewYear}
                onChange={(e) => setViewYear(Number(e.target.value))}
              >
                {years.map((yr) => (
                  <option key={yr} value={yr}>{yr}</option>
                ))}
              </select>
            </div>
            <button type="button" className="datepicker-nav-btn" onClick={() => changeMonth(1)}>
              &rarr;
            </button>
          </div>

          <div className="custom-datepicker-weekdays">
            {['D', 'S', 'T', 'Q', 'Q', 'S', 'S'].map((wd, idx) => (
              <span key={idx} className="datepicker-weekday">{wd}</span>
            ))}
          </div>

          <div className="custom-datepicker-days">
            {prevMonthDaysToShow.map((day, idx) => (
              <span key={`prev-${idx}`} className="datepicker-day datepicker-day-filler">
                {day}
              </span>
            ))}

            {currentMonthDays.map((day) => {
              const isSelected =
                currentDate.getDate() === day &&
                currentDate.getMonth() === viewMonth &&
                currentDate.getFullYear() === viewYear;
              
              const isToday =
                new Date().getDate() === day &&
                new Date().getMonth() === viewMonth &&
                new Date().getFullYear() === viewYear;

              return (
                <span
                  key={`curr-${day}`}
                  className={`datepicker-day ${isSelected ? 'datepicker-day-selected' : ''} ${isToday ? 'datepicker-day-today' : ''}`}
                  onClick={() => selectDay(day)}
                >
                  {day}
                </span>
              );
            })}

            {nextMonthDaysToShow.map((day, idx) => (
              <span key={`next-${idx}`} className="datepicker-day datepicker-day-filler">
                {day}
              </span>
            ))}
          </div>

          <div className="custom-datepicker-footer">
            <button
              type="button"
              className="datepicker-footer-btn"
              onClick={() => {
                const today = new Date();
                const pad = (num) => String(num).padStart(2, '0');
                const todayStr = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
                onChange(todayStr);
                setIsOpen(false);
              }}
            >
              Hoje
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
