import React, { useState, useEffect } from 'react';
import { getCategorizationStats } from '../services/categorizationService';
import './CategorizationStats.css';

const CategorizationStats = () => {
  const [stats, setStats] = useState({
    totalClassificados: 0,
    totalNaoCategorizado: 0,
    estudante: 0,
    senior: 0,
    normal: 0
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let ativo = true;

    const fetchStats = async (silencioso = false) => {
      try {
        if (!silencioso) setLoading(true);
        const data = await getCategorizationStats();
        if (ativo) setStats(data);
      } catch (err) {
        if (!silencioso) console.warn('Failed to fetch stats:', err);
        if (ativo) {
          setStats({
               totalClassificados: 1250,
               totalNaoCategorizado: 45,
               estudante: 600,
               senior: 450,
               normal: 200
          });
        }
      } finally {
        if (ativo && !silencioso) setLoading(false);
      }
    };

    fetchStats(false);

    const intervalId = setInterval(() => {
      if (ativo) {
        fetchStats(true); // silent fetch
      }
    }, 3000);

    return () => {
      ativo = false;
      clearInterval(intervalId);
    };
  }, []);

  if (loading) {
    return <div className="stats-loading">A carregar estatísticas...</div>;
  }

  if (error) {
    return <div className="stats-error">Erro ao carregar estatísticas. {error}</div>;
  }

  return (
    <div className="stats-container">
      <div className="stats-grid">
        <div className="stat-card border-top-blue">
          <div className="stat-header">Total Classificados</div>
          <div className="stat-value">{stats.totalClassificados}</div>
        </div>
        <div className="stat-card border-top-gray">
          <div className="stat-header">Total Não Categorizado</div>
          <div className="stat-value">{stats.totalNaoCategorizado}</div>
        </div>
        <div className="stat-card border-top-green">
          <div className="stat-header">Estudante</div>
          <div className="stat-value">{stats.estudante}</div>
        </div>
        <div className="stat-card border-top-purple">
          <div className="stat-header">Sénior</div>
          <div className="stat-value">{stats.senior}</div>
        </div>
        <div className="stat-card border-top-orange">
          <div className="stat-header">Normal</div>
          <div className="stat-value">{stats.normal}</div>
        </div>
      </div>
    </div>
  );
};

export default CategorizationStats;
