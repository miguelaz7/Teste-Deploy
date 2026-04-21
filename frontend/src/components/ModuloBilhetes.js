import React, { useState } from 'react';
import EcraGestaoMapeamentos from './EcraGestaoMapeamentos';
import EcraRevisaoEventos from './EcraRevisaoEventos';
import './ModuloBilhetes.css';

const ModuloBilhetes = () => {
  const [activeSubTab, setActiveSubTab] = useState('mapeamentos');

  return (
    <div className="modulo-bilhetes-container">
      <div className="bilhetes-nav">
        <button 
          className={`sub-nav-btn ${activeSubTab === 'mapeamentos' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('mapeamentos')}
        >
          Gestão de Mapeamentos
        </button>
        <button 
          className={`sub-nav-btn ${activeSubTab === 'revisao' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('revisao')}
        >
          Revisão de Eventos (Não Categorizados)
        </button>
      </div>

      <div className="bilhetes-content">
        {activeSubTab === 'mapeamentos' && <EcraGestaoMapeamentos />}
        {activeSubTab === 'revisao' && <EcraRevisaoEventos />}
      </div>
    </div>
  );
};

export default ModuloBilhetes;
